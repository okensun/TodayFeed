package com.okensun.todayfeed.components.articles.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.okensun.todayfeed.components.articles.data.database.ArticleEntity
import com.okensun.todayfeed.components.articles.data.database.ArticlesDao
import com.okensun.todayfeed.components.articles.data.database.FeedMetadataEntity
import com.okensun.todayfeed.components.articles.data.source.ArticlesPage
import com.okensun.todayfeed.components.articles.data.source.ArticlesService
import com.okensun.todayfeed.components.articles.data.source.EmptyBodyException
import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.freshness.Connectivity
import com.okensun.todayfeed.core.freshness.decide
import com.okensun.todayfeed.core.freshness.wantsNetwork
import com.okensun.todayfeed.core.network.maxAgeOf
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.time.Clock
import java.time.Duration

/**
 * Fills the article table from the source. Paging decides when to ask for more; this decides
 * whether asking is worth it, using the same function the tests drive directly.
 */
@OptIn(ExperimentalPagingApi::class)
internal class ArticlesRemoteMediator(
    private val service: ArticlesService,
    private val dao: ArticlesDao,
    private val connectivity: Connectivity,
    private val clock: Clock,
    private val pageSize: Int = PAGE_SIZE,
) : RemoteMediator<Int, ArticleEntity>() {
    /** `initialize()` is where the library lets an app read its own cache first, so the
     * freshness decision stays ours and stays the call a unit test makes. */
    override suspend fun initialize(): InitializeAction {
        val metadata = dao.findMetadata()
        val decision =
            decide(
                cachedAt = metadata?.lastRefreshedAt,
                serverMaxAge = metadata?.serverMaxAge,
                timeToLive = FALLBACK_TIME_TO_LIVE,
                connection = connectivity.current(),
                now = clock.instant()
            )
        // A skip is right, but `initialize()` runs once per pager, so nothing here asks again.
        // The screen watching for the network to return is what covers that.
        return if (decision.wantsNetwork) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ArticleEntity>,
    ): MediatorResult {
        // Nothing to ask with no connection. What is stored is what the screen shows, so this is
        // a success with nothing added rather than a failure to report.
        if (connectivity.current() == Connection.Offline) {
            return MediatorResult.Success(endOfPaginationReached = false)
        }
        return when (loadType) {
            // New articles arrive at the front, so a refresh brings them.
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> append()
            LoadType.REFRESH -> refresh()
        }
    }

    /** Where the next page starts comes from what was stored, not from [PagingState]: the source
     * counts offsets, the state counts rows on screen, and a refresh pulls them apart. */
    private suspend fun append(): MediatorResult {
        val metadata = dao.findMetadata()
        // Nothing refreshed yet, or the source has said there is no more. Neither asks.
        if (metadata == null || !metadata.hasMore) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }
        return when (val fetched = fetch(metadata.nextOffset)) {
            is Fetched.Failed -> fetched.result
            is Fetched.Page -> storeAppended(metadata, fetched.body)
        }
    }

    /**
     * Walks forward until a page holds an article already stored, which closes the gap left by
     * time away. Capped, or a reader who has fallen right behind would read the source end to end.
     */
    private suspend fun refresh(): MediatorResult {
        var walk = Walk(previous = dao.findMetadata())
        while (!walk.done && walk.pages < MAX_REFRESH_PAGES) {
            when (val fetched = fetch(walk.fetched)) {
                is Fetched.Failed -> return fetched.result
                is Fetched.Page -> {
                    // Counted before storing, or every page would look familiar.
                    val known = dao.countStored(fetched.body.results.map { it.id.toString() })
                    // Upsert, not delete, so a reader who has scrolled keeps their place.
                    dao.upsertArticles(fetched.body.results.map { it.toEntity() })
                    walk = walk.and(fetched, known)
                }
            }
        }
        return finish(walk)
    }

    private suspend fun finish(walk: Walk): MediatorResult {
        // Only stamp when something was stored. An empty 200 would otherwise buy silence for
        // the whole allowance while the feed shows nothing.
        if (walk.fetched > 0) {
            dao.upsertMetadata(
                FeedMetadataEntity(
                    lastRefreshedAt = clock.instant(),
                    // Every value: Headers.get returns only the last of a repeated header.
                    serverMaxAge = maxAgeOf(walk.stated.joinToString(separator = ", ")),
                    nextOffset = walk.nextOffset,
                    hasMore = !walk.endOfSource
                )
            )
        }
        return MediatorResult.Success(endOfPaginationReached = walk.endOfSource)
    }

    /** How far the refresh has walked. [newlyArrived] is how far down everything the reader
     * already holds has moved. */
    private data class Walk(
        val previous: FeedMetadataEntity?,
        val pages: Int = 0,
        val fetched: Int = 0,
        val newlyArrived: Int = 0,
        val reachedKnown: Boolean = false,
        val endOfSource: Boolean = false,
        val stated: List<String> = emptyList(),
    ) {
        // With nothing held before, one page is the whole answer: there is no gap to close.
        val done: Boolean get() = pages > 0 && (previous == null || reachedKnown || endOfSource)

        // Past the cap the walk never met what the reader holds, so appends carry on from
        // where it stopped instead.
        val nextOffset: Int
            get() = if (reachedKnown) (previous?.nextOffset ?: 0) + newlyArrived else fetched

        fun and(
            page: Fetched.Page,
            known: Int,
        ): Walk =
            copy(
                pages = pages + 1,
                fetched = fetched + page.body.results.size,
                newlyArrived = newlyArrived + page.body.results.size - known,
                reachedKnown = known > 0,
                endOfSource = page.body.next == null || page.body.results.isEmpty(),
                stated = if (pages == 0) page.cacheControl else stated
            )
    }

    /**
     * Paging does not wrap `load`, so a throw takes the whole collection down instead of becoming
     * an error state. Catching broadly is the contract here, not an oversight.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetch(offset: Int): Fetched =
        try {
            val response = service.articles(limit = pageSize, offset = offset)
            val body = response.body()
            when {
                !response.isSuccessful -> Fetched.Failed(MediatorResult.Error(HttpException(response)))
                body == null -> Fetched.Failed(MediatorResult.Error(EmptyBodyException(response.code())))
                else -> Fetched.Page(body, response.headers().values(CACHE_CONTROL))
            }
        } catch (cancelled: CancellationException) {
            // Cancellation is not a failure. A refresh outranks an append and cancels it, and
            // an error there would offer a retry for a load nothing was wrong with.
            @Suppress("RethrowCaughtException")
            throw cancelled
        } catch (e: Throwable) {
            Fetched.Failed(MediatorResult.Error(e))
        }

    /** One page, or the failure to say instead of it. */
    private sealed interface Fetched {
        data class Page(
            val body: ArticlesPage,
            val cacheControl: List<String>,
        ) : Fetched

        data class Failed(
            val result: MediatorResult,
        ) : Fetched
    }

    /**
     * An append brings older articles, so it moves the offset on and leaves the freshness stamp
     * where it was. Stamping it here would let a long read keep buying silence.
     */
    private suspend fun storeAppended(
        metadata: FeedMetadataEntity,
        body: ArticlesPage,
    ): MediatorResult {
        // An empty page stores nothing, so Room never invalidates and Paging never asks again.
        // Without this the reader is parked at the bottom for ever.
        if (body.results.isEmpty()) {
            dao.setPagingProgress(nextOffset = metadata.nextOffset, hasMore = false)
            return MediatorResult.Success(endOfPaginationReached = true)
        }
        dao.upsertArticles(body.results.map { it.toEntity() })
        // Only the paging columns: the whole row would carry a freshness stamp read before
        // this request and revert a refresh that landed meanwhile.
        dao.setPagingProgress(
            nextOffset = metadata.nextOffset + body.results.size,
            hasMore = body.next != null
        )
        return MediatorResult.Success(endOfPaginationReached = body.next == null)
    }

    internal companion object {
        const val PAGE_SIZE = 20

        /** How many pages one refresh may walk back before it accepts the gap. */
        const val MAX_REFRESH_PAGES = 5

        /** Used only when the source states no maximum age of its own. This one always does. */
        val FALLBACK_TIME_TO_LIVE: Duration = Duration.ofMinutes(15)
        const val CACHE_CONTROL = "Cache-Control"
    }
}
