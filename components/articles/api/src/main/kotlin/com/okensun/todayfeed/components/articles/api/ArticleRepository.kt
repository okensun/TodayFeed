package com.okensun.todayfeed.components.articles.api

import androidx.paging.PagingData
import com.okensun.todayfeed.components.articles.api.models.Article
import kotlinx.coroutines.flow.Flow

/**
 * The contract for reading articles. This interface, not the model, is why `api` is its own
 * module: `ui` and `domain` compile against it, `data` implements it, and only `:app` knows which
 * implementation is bound. Nothing outside `data` can reach Retrofit or Room.
 */
interface ArticleRepository {
    /**
     * Every stored article, newest first, as a paged stream. It is not called the feed, because
     * the feed is the mixed list `:components:feed` builds and this is only one part of it.
     * Paging works out which window to hold; what is on screen always comes from what is stored.
     */
    fun observeArticles(): Flow<PagingData<Article>>

    /** Only what the reader saved, most recently saved first. Readable with no network. */
    fun observeSavedArticles(): Flow<List<Article>>

    /** One article from what is stored, or null when nothing has that id. */
    suspend fun findArticle(id: String): Article?

    /**
     * Keeps this article, or lets it go if it was already kept. One call rather than a choice the
     * caller makes: a caller can only go by what it last saw, and two quick taps see the same
     * answer. An id that is not stored changes nothing.
     */
    suspend fun toggleSaved(id: String)
}
