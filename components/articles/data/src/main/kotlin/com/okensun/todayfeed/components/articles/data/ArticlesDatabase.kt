package com.okensun.todayfeed.components.articles.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.okensun.todayfeed.core.database.Converters

/**
 * This component owns its own database. There is no shared schema, so a change to how articles
 * are stored cannot break another component's cache, and no query can span components — which is
 * fine, because the feed is assembled from independent sources rather than joined.
 */
@Database(
    entities = [ArticleEntity::class, FeedMetadataEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
internal abstract class ArticlesDatabase : RoomDatabase() {
    abstract fun dao(): ArticlesDao
}
