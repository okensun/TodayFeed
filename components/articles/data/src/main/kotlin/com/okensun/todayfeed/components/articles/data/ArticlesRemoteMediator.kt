package com.okensun.todayfeed.components.articles.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.okensun.todayfeed.core.freshness.Connectivity
import com.okensun.todayfeed.core.freshness.decide
import com.okensun.todayfeed.core.freshness.wantsNetwork
import com.okensun.todayfeed.core.network.maxAgeOf
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
    /**
     * `initialize()` is the seam the library provides for an app to consult its own cache before
     * the pager refreshes. The freshness decision is therefore ours, and is the same call a unit
     * test makes.
     */
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
        // NothingToServe maps to a skip, which is right in itself — with no network there is
        // nothing to ask. But `initialize()` runs once per pager, so on its own that leaves a
        // reader who started offline with nothing looking at an empty feed until the process
        // restarts. Task 3.4 is what closes it: the screen watches connectivity and refreshes when
        // the network returns. Latent until pass 5 replaces the stand-in that always answers
        // unmetered.
        return if (decision.wantsNetwork) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ArticleEntity>,
    ): MediatorResult =
        when (loadType) {
            // New articles arrive at the front, so a refresh brings them. There is nothing to
            // prepend.
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> append()
            LoadType.REFRESH -> refresh()
        }

    /**
     * Where the next page starts comes from what was stored, not from [PagingState]. The source
     * counts in offsets and the state describes rows on screen, and the two drift apart as soon
     * as a refresh brings articles in above them.
     */
    private suspend fun append(): MediatorResult {
        val metadata = dao.findMetadata()
        // Nothing has been refreshed yet, or the source has already said there is no more. Both
        // mean there is nothing to ask for, so neither makes a request.
        return if (metadata == null || !metadata.hasMore) {
            MediatorResult.Success(endOfPaginationReached = true)
        } else {
            fetch(offset = metadata.nextOffset) { body, _ -> storeAppended(metadata, body) }
        }
    }

    /**
     * Paging does not wrap `load` in a try/catch, so anything thrown here escapes the coroutine
     * and takes the paging collection down instead of becoming an error state. The contract asks
     * for a result rather than a throw, so this catches broadly on purpose: the usual rule about
     * catching specific exceptions does not apply where the framework wants every failure back as
     * a value. A captive portal answering 200 with HTML, or the source dropping a field, arrive
     * here as a serialization failure rather than as an IOException.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetch(
        offset: Int,
        store: suspend (ArticlesPage, List<String>) -> MediatorResult,
    ): MediatorResult =
        try {
            val response = service.articles(limit = pageSize, offset = offset)
            val body = response.body()
            when {
                !response.isSuccessful -> MediatorResult.Error(HttpException(response))
                body == null -> MediatorResult.Error(EmptyBodyException(response.code()))
                else -> store(body, response.headers().values(CACHE_CONTROL))
            }
        } catch (e: Throwable) {
            MediatorResult.Error(e)
        }

    private suspend fun refresh(): MediatorResult = fetch(offset = 0, store = ::storeRefreshed)

    /**
     * An append brings older articles, so it moves the offset on and leaves the freshness stamp
     * where it was. Stamping it here would let a long read keep buying silence.
     */
    private suspend fun storeAppended(
        metadata: FeedMetadataEntity,
        body: ArticlesPage,
    ): MediatorResult {
        dao.upsertArticles(body.results.map { it.toEntity() })
        dao.upsertMetadata(
            metadata.copy(
                nextOffset = metadata.nextOffset + body.results.size,
                hasMore = body.next != null
            )
        )
        return MediatorResult.Success(endOfPaginationReached = body.next == null)
    }

    private suspend fun storeRefreshed(
        body: ArticlesPage,
        cacheControl: List<String>,
    ): MediatorResult {
        // Upsert rather than delete, so a reader who has scrolled keeps what they have and Paging
        // reloads around where they are.
        dao.upsertArticles(body.results.map { it.toEntity() })

        // Stamp the freshness time only when something was actually stored. A 200 carrying an
        // empty list would otherwise buy silence for the whole allowance while the feed shows
        // nothing, and this source holds tens of thousands of articles, so an empty page means
        // something is wrong rather than that there is nothing to read.
        if (body.results.isNotEmpty()) {
            dao.upsertMetadata(
                FeedMetadataEntity(
                    lastRefreshedAt = clock.instant(),
                    // Every value, not the last one. A repeated header would otherwise lose the
                    // stated age, because Headers.get returns only the final occurrence.
                    serverMaxAge = maxAgeOf(cacheControl.joinToString(separator = ", ")),
                    // The offset starts again from this page. A reader who had paged deeper will
                    // therefore be served offsets they already hold on the next append, which the
                    // upsert makes harmless but not free. Counting how many of a page are already
                    // stored is what fixes it, and is the same count task 2.3 needs.
                    nextOffset = body.results.size,
                    hasMore = body.next != null
                )
            )
        }
        return MediatorResult.Success(endOfPaginationReached = body.next == null)
    }

    internal companion object {
        const val PAGE_SIZE = 20

        /** Used only when the source states no maximum age of its own. This one always does. */
        val FALLBACK_TIME_TO_LIVE: Duration = Duration.ofMinutes(15)
        const val CACHE_CONTROL = "Cache-Control"
    }
}
