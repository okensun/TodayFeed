package com.okensun.todayfeed.components.articles.data

import androidx.room.Room
import com.okensun.todayfeed.components.articles.data.database.ArticleEntity
import com.okensun.todayfeed.components.articles.data.database.ArticlesDatabase
import com.okensun.todayfeed.components.articles.data.network.FakeArticlesService
import com.okensun.todayfeed.core.testing.FakeClock
import com.okensun.todayfeed.core.testing.FakeConnectivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class DefaultArticleRepositoryTest {
    private val clock = FakeClock()

    private lateinit var database: ArticlesDatabase
    private lateinit var repository: DefaultArticleRepository

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    ArticlesDatabase::class.java
                ).build()
        repository =
            DefaultArticleRepository(
                database = database,
                service = FakeArticlesService(),
                connectivity = FakeConnectivity(),
                clock = clock
            )
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `keeping puts the article in the saved list and keeping again takes it out`() =
        runTest {
            store("a1")

            repository.toggleSaved("a1")
            assertEquals(listOf("a1"), repository.observeSavedArticles().first().map { it.id })

            repository.toggleSaved("a1")
            assertTrue(repository.observeSavedArticles().first().isEmpty())
        }

    /** The order is when it was saved, which is why the repository is the one holding the clock. */
    @Test
    fun `the saved list reads most recently saved first`() =
        runTest {
            store("first")
            store("second")

            repository.toggleSaved("first")
            clock.advanceBy(Duration.ofMinutes(1))
            repository.toggleSaved("second")

            assertEquals(
                listOf("second", "first"),
                repository.observeSavedArticles().first().map { it.id }
            )
        }

    @Test
    fun `a saved article is still readable when it is no longer in the feed`() =
        runTest {
            store("gone")
            repository.toggleSaved("gone")

            assertEquals("gone", repository.findArticle("gone")?.id)
        }

    private suspend fun store(id: String) =
        database.dao().upsertArticles(
            listOf(
                ArticleEntity(
                    id = id,
                    title = "Article $id",
                    summary = "Summary",
                    source = "NASA",
                    imageUrl = null,
                    publishedAt = Instant.EPOCH
                )
            )
        )
}
