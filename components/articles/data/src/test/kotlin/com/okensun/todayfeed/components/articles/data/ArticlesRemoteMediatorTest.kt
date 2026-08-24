package com.okensun.todayfeed.components.articles.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import com.okensun.todayfeed.core.testing.FakeClock
import com.okensun.todayfeed.core.testing.FakeConnectivity
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

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class ArticlesRemoteMediatorTest {
    private val service = FakeArticlesService()
    private val clock = FakeClock()
    private val connectivity = FakeConnectivity()

    private lateinit var database: ArticlesDatabase
    private lateinit var dao: ArticlesDao
    private lateinit var mediator: ArticlesRemoteMediator

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    ArticlesDatabase::class.java
                ).build()
        dao = database.dao()
        mediator =
            ArticlesRemoteMediator(
                service = service,
                dao = dao,
                connectivity = connectivity,
                clock = clock,
                // Two, so an offset in an assertion is small enough to read.
                pageSize = PAGE
            )
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `an append asks for the page after the one already stored`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            service.enqueue(page(3..4, hasNext = false))
            val result = mediator.load(LoadType.APPEND, noState())

            assertEquals(listOf(0, 2), service.requests.map { it.offset })
            assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
            assertEquals(4, dao.countStored(listOf("1", "2", "3", "4")))
        }

    /** The point of storing `hasMore`: the end of the source is known without asking again. */
    @Test
    fun `an append past the end of the source makes no request`() =
        runTest {
            service.enqueue(page(1..2, hasNext = false))
            mediator.load(LoadType.REFRESH, noState())
            val asked = service.requests.size

            val result = mediator.load(LoadType.APPEND, noState())

            assertEquals(asked, service.requests.size)
            assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        }

    /**
     * Scrolling reads older articles, which says nothing about whether the front of the feed has
     * changed. If an append stamped the freshness time, a long read would keep buying silence and
     * the reader would stop being told about anything new.
     */
    @Test
    fun `an append does not make the feed look fresher`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            val refreshedAt = dao.findMetadata()?.lastRefreshedAt

            clock.advanceBy(Duration.ofMinutes(30))
            service.enqueue(page(3..4, hasNext = false))
            mediator.load(LoadType.APPEND, noState())

            assertEquals(refreshedAt, dao.findMetadata()?.lastRefreshedAt)
        }

    @Test
    fun `an append with nothing refreshed yet makes no request`() =
        runTest {
            val result = mediator.load(LoadType.APPEND, noState())

            assertEquals(emptyList<Int>(), service.requests.map { it.offset })
            assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        }

    /**
     * Task 2.2. A refresh upserts rather than deletes, so what the reader has already paged
     * through stays where it was and the new articles arrive above it.
     */
    @Test
    fun `a refresh after three pages keeps them and puts the new articles on top`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.enqueue(page(3..4, hasNext = true))
            mediator.load(LoadType.APPEND, noState())
            service.enqueue(page(5..6, hasNext = true))
            mediator.load(LoadType.APPEND, noState())

            service.enqueue(page(7..8, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            assertEquals(listOf("8", "7", "6", "5", "4", "3", "2", "1"), storedNewestFirst())
        }

    private suspend fun storedNewestFirst(): List<String> {
        val page =
            dao.pagedArticles().load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 50, placeholdersEnabled = false)
            )
        return (page as PagingSource.LoadResult.Page).data.map { it.id }
    }

    private fun noState() =
        PagingState<Int, ArticleEntity>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = PAGE),
            leadingPlaceholderCount = 0
        )

    /** Ids double as order: a higher number is a later article. */
    private fun page(
        ids: IntRange,
        hasNext: Boolean,
    ) = ArticlesPage(
        count = ids.count(),
        next = if (hasNext) "https://example.test/next" else null,
        results =
            ids.map { id ->
                ArticleDto(
                    id = id,
                    title = "Article $id",
                    summary = "Summary $id",
                    newsSite = "Spaceflight News",
                    imageUrl = null,
                    publishedAt = EPOCH.plus(Duration.ofMinutes(id.toLong())).toString()
                )
            }
    )

    private companion object {
        const val PAGE = 2
        val EPOCH: Instant = Instant.parse("2026-08-01T00:00:00Z")
    }
}
