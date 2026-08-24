package com.okensun.todayfeed.components.articles.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.testing.FakeClock
import com.okensun.todayfeed.core.testing.FakeConnectivity
import kotlinx.coroutines.CancellationException
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
     * Scrolling reads older articles, which says nothing about the front of the feed. Stamping
     * the time here would let a long read keep buying silence.
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
     * A refresh outranks an append and cancels it. Cancellation is not a failure, and an error
     * there offers a retry for a load nothing was wrong with.
     */
    @Test
    fun `a cancelled append is not turned into an error`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            service.enqueueThrowing(CancellationException("a refresh took priority"))
            val outcome = runCatching { mediator.load(LoadType.APPEND, noState()) }

            assertTrue(outcome.exceptionOrNull() is CancellationException)
        }

    /**
     * A page with nothing in it stores nothing, so Room does not invalidate and Paging is never
     * asked again. Without this the reader is parked at the bottom asking for the same offset.
     */
    @Test
    fun `an append that comes back empty is the end rather than a stall`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            service.enqueue(page(IntRange.EMPTY, hasNext = true))
            val result = mediator.load(LoadType.APPEND, noState())

            assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
            assertEquals(false, dao.findMetadata()?.hasMore)
        }

    /**
     * Task 3.1. With no connection there is nothing to ask. What is stored is what the screen
     * shows, so this is a success with nothing added rather than an error.
     */
    @Test
    fun `with no connection a refresh asks for nothing and keeps what is stored`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.requests.clear()

            connectivity.set(Connection.Offline)
            val result = mediator.load(LoadType.REFRESH, noState())

            assertEquals(emptyList<Int>(), service.requests.map { it.offset })
            assertTrue(result is RemoteMediator.MediatorResult.Success)
            assertEquals(listOf("2", "1"), storedNewestFirst())
        }

    @Test
    fun `with no connection an append asks for nothing`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.requests.clear()

            connectivity.set(Connection.Offline)
            val result = mediator.load(LoadType.APPEND, noState())

            assertEquals(emptyList<Int>(), service.requests.map { it.offset })
            assertTrue(result is RemoteMediator.MediatorResult.Success)
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

            service.enqueue(page(listOf(8, 7), hasNext = true))
            service.enqueue(page(listOf(6, 5), hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            assertEquals(listOf("8", "7", "6", "5", "4", "3", "2", "1"), storedNewestFirst())
        }

    /**
     * Task 2.3. Time away leaves a gap between what the reader holds and what the source has.
     * Walking until a page holds something familiar closes it without guessing its width.
     */
    @Test
    fun `a refresh keeps asking until a page holds something already stored`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.requests.clear()

            service.enqueue(page(listOf(9, 8), hasNext = true))
            service.enqueue(page(listOf(7, 6), hasNext = true))
            service.enqueue(page(listOf(5, 2), hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            assertEquals(listOf(0, 2, 4), service.requests.map { it.offset })
            assertEquals(listOf("9", "8", "7", "6", "5", "2", "1"), storedNewestFirst())
        }

    @Test
    fun `a refresh whose first page is already familiar asks once`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.requests.clear()

            service.enqueue(page(listOf(3, 2), hasNext = true))
            mediator.load(LoadType.REFRESH, noState())

            assertEquals(listOf(0), service.requests.map { it.offset })
        }

    /**
     * The cap stops a source that has moved on entirely from being read end to end in one
     * refresh. It leaves a gap, which the appends after it fill from where the walk stopped.
     */
    @Test
    fun `a refresh gives up after five pages rather than reading the whole source`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.requests.clear()

            repeat(6) { service.enqueue(page(listOf(100 - it * 2, 99 - it * 2), hasNext = true)) }
            mediator.load(LoadType.REFRESH, noState())

            assertEquals(listOf(0, 2, 4, 6, 8), service.requests.map { it.offset })
        }

    /**
     * What the reader already holds moves down by however many articles arrived above it, so the
     * next append starts below that rather than at the page the refresh happened to end on.
     */
    @Test
    fun `the next append carries on below what the reader already had`() =
        runTest {
            service.enqueue(page(1..2, hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.enqueue(page(3..4, hasNext = true))
            mediator.load(LoadType.APPEND, noState())
            service.requests.clear()

            // Two arrive above what is held, and the second page meets a familiar article.
            service.enqueue(page(listOf(9, 8), hasNext = true))
            service.enqueue(page(listOf(4, 3), hasNext = true))
            mediator.load(LoadType.REFRESH, noState())
            service.enqueue(page(listOf(0), hasNext = false))
            mediator.load(LoadType.APPEND, noState())

            assertEquals(listOf(0, 2, 6), service.requests.map { it.offset })
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

    private fun page(
        ids: IntRange,
        hasNext: Boolean,
    ) = page(ids.toList(), hasNext)

    /** Ids double as order: a higher number is a later article. */
    private fun page(
        ids: List<Int>,
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
