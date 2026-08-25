package com.okensun.todayfeed.components.articles.data.database

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Upgrading must not throw away what the reader saved, which is the requirement this change
 * adds. A destructive fallback would, so the migration is written and this proves it runs.
 */
@RunWith(RobolectricTestRunner::class)
class ArticlesMigrationTest {
    @Test
    fun `an article stored before saving existed survives the upgrade`() =
        runTest {
            writeVersionOne()

            val database =
                Room
                    .databaseBuilder(
                        RuntimeEnvironment.getApplication(),
                        ArticlesDatabase::class.java,
                        NAME
                    ).addMigrations(MIGRATION_1_2)
                    .build()

            val article = database.dao().findArticle("a1")
            assertEquals("Before saving existed", article?.title)
            assertNull("nothing was saved before the column existed", article?.savedAt)
            database.close()
        }

    /** The version 1 schema, written by hand because version 1 exported none. */
    private fun writeVersionOne() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(NAME)
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(NAME)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `articles` (`id` TEXT NOT NULL, " +
                                        "`title` TEXT NOT NULL, `summary` TEXT NOT NULL, " +
                                        "`source` TEXT NOT NULL, `imageUrl` TEXT, " +
                                        "`publishedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                                )
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `feed_metadata` (`id` INTEGER NOT NULL, " +
                                        "`lastRefreshedAt` INTEGER NOT NULL, `serverMaxAge` INTEGER, " +
                                        "`nextOffset` INTEGER NOT NULL, `hasMore` INTEGER NOT NULL, " +
                                        "PRIMARY KEY(`id`))"
                                )
                                db.execSQL(
                                    "INSERT INTO articles VALUES " +
                                        "('a1', 'Before saving existed', 'Summary', 'NASA', NULL, 0)"
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        }
                    ).build()
            )
        helper.writableDatabase.close()
        helper.close()
    }

    private companion object {
        const val NAME = "migration-articles.db"
    }
}
