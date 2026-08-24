package com.okensun.todayfeed.components.articles.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Keyed on the article's own id, and ordered by when it was published. Neither is a fact about
 * how it was fetched, so fetching the same article twice at different offsets overwrites one row
 * rather than adding a second.
 */
@Entity(tableName = "articles")
internal data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val imageUrl: String?,
    val publishedAt: Instant,
    /** When the reader saved it, or null when they have not. Also the order of the Saved tab. */
    val savedAt: Instant? = null,
)
