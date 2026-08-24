package com.okensun.todayfeed.components.articles.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.core.freshness.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads only from Room. The mediator writes to Room and this reads from it, so a refresh that
 * fails cannot empty the screen and the offline case needs no separate branch.
 */
@Singleton
internal class DefaultArticleRepository
    @Inject
    constructor(
        private val database: ArticlesDatabase,
        private val service: ArticlesService,
        private val connectivity: Connectivity,
        private val clock: Clock,
    ) : ArticleRepository {
        @OptIn(ExperimentalPagingApi::class)
        override fun observeArticles(): Flow<PagingData<Article>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = ArticlesRemoteMediator.PAGE_SIZE,
                        // Start the next page before the reader reaches the end, so item twenty-one does not
                        // wait. Pass 5 takes this number from the connection.
                        prefetchDistance = PREFETCH_DISTANCE,
                        enablePlaceholders = false
                    ),
                remoteMediator =
                    ArticlesRemoteMediator(
                        service = service,
                        dao = database.dao(),
                        connectivity = connectivity,
                        clock = clock
                    ),
                pagingSourceFactory = { database.dao().pagedArticles() }
            ).flow.map { page -> page.map { it.toArticle() } }

        // Saving arrives in slice 3. Until then nothing is saved, which is what this says.
        override fun observeSavedArticles(): Flow<List<Article>> = flowOf(emptyList())

        override suspend fun findArticle(id: String): Article? = database.dao().findArticle(id)?.toArticle()

        private companion object {
            const val PREFETCH_DISTANCE = 5
        }
    }
