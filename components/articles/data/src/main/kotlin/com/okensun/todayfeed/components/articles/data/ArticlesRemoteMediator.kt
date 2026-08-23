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
import java.io.IOException
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

    private suspend fun refresh(): MediatorResult =
        try {
            val response = service.articles(limit = pageSize, offset = 0)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                MediatorResult.Error(HttpException(response))
            } else {
                // Upsert rather than delete, so a reader who has scrolled keeps what they have and
                // Paging reloads around where they are.
                dao.upsertArticles(body.results.map { it.toEntity() })
                dao.upsertMetadata(
                    FeedMetadataEntity(
                        lastRefreshedAt = clock.instant(),
                        serverMaxAge = maxAgeOf(response.headers()[CACHE_CONTROL]),
                        nextOffset = body.results.size,
                        hasMore = body.next != null
                    )
                )
                MediatorResult.Success(endOfPaginationReached = body.next == null)
            }
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }

    internal companion object {
        const val PAGE_SIZE = 20

        /** Used only when the source states no maximum age of its own. This one always does. */
        val FALLBACK_TIME_TO_LIVE: Duration = Duration.ofMinutes(15)
        private const val CACHE_CONTROL = "Cache-Control"
    }
}
