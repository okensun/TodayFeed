package com.okensun.todayfeed.components.articles.api

import kotlinx.coroutines.flow.Flow

/**
 * The contract for reading articles. This interface, not the model, is why `api` is its own
 * module: `ui` and `domain` compile against it, `data` implements it, and only `:app` knows
 * which implementation is bound. Nothing outside `data` can reach Retrofit or Room.
 *
 * Slice 2 adds paging and the freshness policy behind these calls. The shape stays.
 */
interface ArticleRepository {
    /** The feed, newest first. Emits again whenever the cache changes. */
    fun observeFeed(): Flow<List<Article>>

    /** Only what the user saved. Readable with no network once saved. */
    fun observeSaved(): Flow<List<Article>>

    /** One article, or null when no article has that id. */
    suspend fun article(id: String): Article?
}
