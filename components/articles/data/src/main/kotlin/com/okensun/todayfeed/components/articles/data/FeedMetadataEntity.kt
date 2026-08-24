package com.okensun.todayfeed.components.articles.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.Instant

/**
 * One row, ever. It holds what the freshness decision needs: when the feed was last refreshed and
 * the maximum age the source stated for itself. Keeping it in the same database as the articles
 * means the age and the data cannot disagree.
 */
@Entity(tableName = "feed_metadata")
internal data class FeedMetadataEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val lastRefreshedAt: Instant,
    val serverMaxAge: Duration?,
    val nextOffset: Int,
    val hasMore: Boolean,
) {
    internal companion object {
        const val SINGLE_ROW = 0
    }
}
