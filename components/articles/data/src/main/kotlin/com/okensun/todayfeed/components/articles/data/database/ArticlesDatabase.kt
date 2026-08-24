package com.okensun.todayfeed.components.articles.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.okensun.todayfeed.core.database.Converters

/**
 * This component owns its own database. There is no shared schema, so a change to how articles
 * are stored cannot break another component's cache, and no query can span components — which is
 * fine, because the feed is assembled from independent sources rather than joined.
 */
@Database(
    entities = [ArticleEntity::class, FeedMetadataEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
internal abstract class ArticlesDatabase : RoomDatabase() {
    abstract fun dao(): ArticlesDao
}

/**
 * One nullable column. A destructive fallback would be shorter and would throw away exactly what
 * this version added the ability to keep.
 */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE articles ADD COLUMN savedAt INTEGER")
        }
    }
