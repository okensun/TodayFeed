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
        val metadata = dao.metadata()
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
            // Pass 2 implements this. Until then the feed is one page, which is still real articles
            // rather than placeholders.
            LoadType.APPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.REFRESH -> refresh()
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
    private suspend fun refresh(): MediatorResult =
        try {
            val response = service.articles(limit = pageSize, offset = 0)
            val body = response.body()
            when {
                !response.isSuccessful -> MediatorResult.Error(HttpException(response))
                body == null -> MediatorResult.Error(EmptyBodyException(response.code()))
                else -> store(body, response.headers().values(CACHE_CONTROL))
            }
        } catch (e: Throwable) {
            MediatorResult.Error(e)
        }

    private suspend fun store(
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
