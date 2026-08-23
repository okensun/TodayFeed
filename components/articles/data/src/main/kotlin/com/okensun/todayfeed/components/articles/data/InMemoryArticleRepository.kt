package com.okensun.todayfeed.components.articles.data

import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a fixed list in memory so the walking skeleton has something to draw. Slice 2
 * replaces the body with Retrofit and Room behind the freshness policy. Callers do not
 * change, which is the point of the interface living in `api`.
 */
@Singleton
class InMemoryArticleRepository
    @Inject
    constructor() : ArticleRepository {
        private val articles =
            MutableStateFlow(
                List(PLACEHOLDER_COUNT) { index ->
                    Article(
                        id = "placeholder-$index",
                        title = "Placeholder article ${index + 1}",
                        summary = "Real articles arrive in slice 2, from Spaceflight News.",
                        source = "Spaceflight News",
                        imageUrl = null,
                        publishedAt = Instant.EPOCH
                    )
                }
            )

        override fun observeFeed(): Flow<List<Article>> = articles.asStateFlow()

        override fun observeSaved(): Flow<List<Article>> = articles.map { it.take(1) }

        override suspend fun article(id: String): Article? = articles.value.firstOrNull { it.id == id }

        private companion object {
            const val PLACEHOLDER_COUNT = 6
        }
    }
