package com.okensun.todayfeed.components.articles.data

import androidx.paging.PagingSource
import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ArticlesDaoTest {
    private lateinit var database: ArticlesDatabase
    private lateinit var dao: ArticlesDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    ArticlesDatabase::class.java
                ).build()
        dao = database.dao()
    }

    @After
    fun close() {
        database.close()
    }

    /** The point of keying on the article id: fetching the same article twice is not a duplicate. */
    @Test
    fun `the same article written twice leaves one row holding the newer content`() =
        runTest {
            dao.upsertArticles(listOf(article("a1", title = "First title")))
            dao.upsertArticles(listOf(article("a1", title = "Corrected title")))

            assertEquals("Corrected title", dao.findArticle("a1")?.title)
            assertEquals(1, dao.countStored(listOf("a1")))
        }

    @Test
    fun `articles come back newest first, whatever order they were written in`() =
        runTest {
            dao.upsertArticles(
                listOf(
                    article("old", publishedAt = EPOCH),
                    article("new", publishedAt = EPOCH.plus(Duration.ofDays(2))),
                    article("middle", publishedAt = EPOCH.plus(Duration.ofDays(1)))
                )
            )

            val page =
                dao.pagedArticles().load(
                    PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
                )

            assertTrue(page is PagingSource.LoadResult.Page)
            assertEquals(
                listOf("new", "middle", "old"),
                (page as PagingSource.LoadResult.Page).data.map { it.id }
            )
        }

    @Test
    fun `counting stored ids is how a refresh knows it has reached back far enough`() =
        runTest {
            dao.upsertArticles(listOf(article("a1"), article("a2")))

            assertEquals(0, dao.countStored(listOf("a9", "a8")))
            assertEquals(1, dao.countStored(listOf("a1", "a9")))
            assertEquals(2, dao.countStored(listOf("a1", "a2")))
        }

    @Test
    fun `metadata is one row and an Instant and a Duration survive it`() =
        runTest {
            assertNull(dao.findMetadata())

            dao.upsertMetadata(
                FeedMetadataEntity(
                    lastRefreshedAt = Instant.parse("2026-08-23T12:00:00Z"),
                    serverMaxAge = Duration.ofMinutes(10),
                    nextOffset = 20,
                    hasMore = true
                )
            )
            dao.upsertMetadata(
                FeedMetadataEntity(
                    lastRefreshedAt = Instant.parse("2026-08-23T12:30:00Z"),
                    serverMaxAge = null,
                    nextOffset = 40,
                    hasMore = false
                )
            )

            val stored = dao.findMetadata()
            assertEquals(Instant.parse("2026-08-23T12:30:00Z"), stored?.lastRefreshedAt)
            assertNull(stored?.serverMaxAge)
            assertEquals(40, stored?.nextOffset)
            assertEquals(false, stored?.hasMore)
        }

    private fun article(
        id: String,
        title: String = "Article $id",
        publishedAt: Instant = EPOCH,
    ) = ArticleEntity(
        id = id,
        title = title,
        summary = "Summary $id",
        source = "Spaceflight News",
        imageUrl = null,
        publishedAt = publishedAt
    )

    private companion object {
        val EPOCH: Instant = Instant.parse("2026-08-01T00:00:00Z")
    }
}
