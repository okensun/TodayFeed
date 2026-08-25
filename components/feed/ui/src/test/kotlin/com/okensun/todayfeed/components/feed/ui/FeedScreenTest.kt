package com.okensun.todayfeed.components.feed.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.okensun.todayfeed.components.articles.api.models.Article
import com.okensun.todayfeed.components.feed.domain.FeedSection
import com.okensun.todayfeed.components.weather.api.models.Weather
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class FeedScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sections are drawn above the articles`() {
        show(articles = listOf(article("a1", "Article one")), sections = listOf(hero))

        val section = compose.onNodeWithText("Taipei").getBoundsInRoot()
        val first = compose.onNodeWithText("Article one").getBoundsInRoot()

        assertTrue("section at ${section.top}, article at ${first.top}", section.top < first.top)
    }

    /** Every band of the feed is named, so a reader can tell one kind of content from another. */
    @Test
    fun `the articles are under a heading of their own`() {
        show(articles = listOf(article("a1", "Article one")), sections = listOf(hero))

        val heading = compose.onNodeWithText(ARTICLES).getBoundsInRoot()
        val first = compose.onNodeWithText("Article one").getBoundsInRoot()

        assertTrue("heading at ${heading.top}, article at ${first.top}", heading.top < first.top)
    }

    /** A heading over nothing is worse than no heading. */
    @Test
    fun `with no articles there is no articles heading`() {
        show(articles = emptyList(), sections = listOf(hero))

        compose.onNodeWithText(ARTICLES).assertDoesNotExist()
    }

    @Test
    fun `loading says so and shows no articles`() {
        show(articles = emptyList(), refresh = LoadState.Loading)

        compose.onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun `nothing at all says there is nothing to read`() {
        show(articles = emptyList())

        compose.onNodeWithText("Nothing to read yet").assertIsDisplayed()
    }

    @Test
    fun `a failure with nothing loaded offers a retry`() {
        show(articles = emptyList(), refresh = LoadState.Error(RuntimeException("no network")))

        compose.onNodeWithText("no network").assertIsDisplayed()
        compose.onNodeWithText("Try again").assertIsDisplayed()
    }

    /** A weather card with no articles shows the weather card rather than an empty screen. */
    @Test
    fun `a section with no articles is shown, not the empty state`() {
        show(articles = emptyList(), sections = listOf(hero))

        compose.onNodeWithText("Taipei").assertIsDisplayed()
        compose.onNodeWithText("Nothing to read yet").assertDoesNotExist()
    }

    @Test
    fun `tapping an article passes its id`() {
        var clicked: String? = null
        show(articles = listOf(article("a9", "Article nine")), onArticleClick = { clicked = it })

        compose.onNodeWithText("Article nine").performClick()

        assertEquals("a9", clicked)
    }

    /**
     * A refresh with content already on screen is not the loading state. The pull indicator says
     * it is happening; the articles stay, which is the part that can be got wrong here.
     */
    @Test
    fun `a refresh with articles on screen keeps them rather than replacing them`() {
        show(articles = listOf(article("a1", "Article one")), refresh = LoadState.Loading)

        compose.onNodeWithText("Article one").assertIsDisplayed()
        compose.onNodeWithText("Loading").assertDoesNotExist()
    }

    @Test
    fun `a refresh with nothing on screen is the loading state`() {
        show(articles = emptyList(), refresh = LoadState.Loading)

        compose.onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun `an append in progress is shown under the articles`() {
        show(articles = listOf(article("a1", "Article one")), append = LoadState.Loading)

        val last = compose.onNodeWithText("Article one").getBoundsInRoot()
        val appending = compose.onNodeWithContentDescription(APPENDING).getBoundsInRoot()

        assertTrue("article at ${last.top}, indicator at ${appending.top}", last.top < appending.top)
    }

    /**
     * The failure is at the bottom of the list, not the whole screen. Blanking what the reader is
     * holding because the next page did not arrive loses more than it explains.
     */
    @Test
    fun `an append that fails keeps the articles and offers a retry`() {
        show(
            articles = listOf(article("a1", "Article one")),
            append = LoadState.Error(RuntimeException("no network"))
        )

        compose.onNodeWithText("Article one").assertIsDisplayed()
        compose.onNodeWithText("no network").assertIsDisplayed()
        compose.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun `an append that fails is shown under the articles`() {
        show(
            articles = listOf(article("a1", "Article one")),
            append = LoadState.Error(RuntimeException("no network"))
        )

        val last = compose.onNodeWithText("Article one").getBoundsInRoot()
        val failure = compose.onNodeWithText("Try again").getBoundsInRoot()

        assertTrue("article at ${last.top}, retry at ${failure.top}", last.top < failure.top)
    }

    @Test
    fun `with nothing loading the appending indicator is not drawn`() {
        show(articles = listOf(article("a1", "Article one")))

        compose.onNodeWithContentDescription(APPENDING).assertDoesNotExist()
    }

    /**
     * Offline with articles is not a failure. They stay, with a line saying they may be old, so
     * the reader knows what they are looking at rather than being sent away from it.
     */
    @Test
    fun `offline with articles keeps them and says they may be out of date`() {
        show(articles = listOf(article("a1", "Article one")), offline = true)

        compose.onNodeWithText("Article one").assertIsDisplayed()
        compose.onNodeWithText(OUT_OF_DATE).assertIsDisplayed()
    }

    @Test
    fun `online shows no out of date line`() {
        show(articles = listOf(article("a1", "Article one")))

        compose.onNodeWithText(OUT_OF_DATE).assertDoesNotExist()
    }

    @Test
    fun `the out of date line is above the articles`() {
        show(articles = listOf(article("a1", "Article one")), offline = true)

        val line = compose.onNodeWithText(OUT_OF_DATE).getBoundsInRoot()
        val first = compose.onNodeWithText("Article one").getBoundsInRoot()

        assertTrue("line at ${line.top}, article at ${first.top}", line.top < first.top)
    }

    /**
     * The connection is lost while reading, which is not the same as opening the app offline. As
     * a list item the line sat above the key the list anchors on, so it was added and then left
     * off screen until the reader scrolled up to find it.
     */
    @Test
    fun `losing the connection part way down the list still shows the line`() {
        var offline by mutableStateOf(false)
        val many = (1..20).map { article("a$it", "Article $it") }
        compose.setContent {
            TodayFeedTheme {
                FeedScreen(
                    articles = flowOf(PagingData.from(many)),
                    sections = listOf(hero),
                    offline = offline,
                    onArticleClick = {}
                )
            }
        }
        compose.onNodeWithText("Article 1").performTouchInput { swipeUp() }
        compose.waitForIdle()

        offline = true

        compose.onNodeWithText(OUT_OF_DATE).assertIsDisplayed()
    }

    /**
     * The weather arriving after the reader is already looking. Without a nudge the list holds
     * its anchor and the card is inserted above the screen, where nobody finds it.
     */
    @Test
    fun `a section that arrives late is brought into view at the top`() {
        var sections by mutableStateOf(emptyList<FeedSection>())
        compose.setContent {
            TodayFeedTheme {
                FeedScreen(
                    articles = flowOf(PagingData.from((1..20).map { article("a$it", "Article $it") })),
                    sections = sections,
                    offline = false,
                    onArticleClick = {}
                )
            }
        }

        sections = listOf(hero)

        compose.onNodeWithText("Taipei").assertIsDisplayed()
    }

    /** The other half: a reader who has scrolled away is not dragged back to the top. */
    @Test
    fun `a section that arrives late does not move a reader who has scrolled`() {
        var sections by mutableStateOf(emptyList<FeedSection>())
        val listState = LazyListState()
        compose.setContent {
            TodayFeedTheme {
                FeedScreen(
                    articles = flowOf(PagingData.from((1..20).map { article("a$it", "Article $it") })),
                    sections = sections,
                    offline = false,
                    onArticleClick = {},
                    listState = listState
                )
            }
        }
        compose.runOnIdle { }
        compose.waitForIdle()
        runBlocking { listState.scrollToItem(10) }
        compose.waitForIdle()

        sections = listOf(hero)
        compose.waitForIdle()

        assertTrue(
            "first visible was ${listState.firstVisibleItemIndex}",
            listState.firstVisibleItemIndex > 1
        )
    }

    /**
     * The half of the network-returned fix that lives on the screen. The repository knows what to
     * do when asked; nothing else checks that anybody asks.
     */
    @Test
    fun `the network coming back asks the sections again`() {
        var asked = 0
        // Buffered so the test can hand a value over without waiting for the collector, which
        // runs on the thread the test is holding.
        val returned = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        show(
            articles = listOf(article("a1", "Article one")),
            refreshWhen = returned,
            onRefreshSections = { asked++ }
        )

        returned.tryEmit(Unit)
        compose.waitForIdle()

        assertEquals(1, asked)
    }

    @Test
    fun `pulling the list down asks the sections again`() {
        var asked = 0
        show(articles = listOf(article("a1", "Article one")), onRefreshSections = { asked++ })

        compose.onRoot().performTouchInput {
            swipeDown(startY = top + 10f, endY = bottom - 10f, durationMillis = 400)
        }
        compose.waitForIdle()

        assertEquals(1, asked)
    }

    /** The star is drawn by the articles component but tapped here, which is its own wiring. */
    @Test
    fun `tapping the star in the feed passes the article back`() {
        var toggled: Article? = null
        show(articles = listOf(article("a9", "Article nine")), onToggleSave = { toggled = it })

        compose.onNodeWithContentDescription("Save").performClick()

        assertEquals("a9", toggled?.id)
    }

    @Test
    fun `offline with nothing stored says so and offers a retry`() {
        show(articles = emptyList(), offline = true)

        compose.onNodeWithText("You are offline").assertIsDisplayed()
        compose.onNodeWithText("Try again").assertIsDisplayed()
    }

    // Six named defaults is what lets each test say only what it is about. Bundling them into a
    // holder would move that noise into every call site instead of removing it.
    @Suppress("LongParameterList")
    private fun show(
        articles: List<Article>,
        sections: List<FeedSection> = emptyList(),
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        offline: Boolean = false,
        onArticleClick: (String) -> Unit = {},
        onRefreshSections: () -> Unit = {},
        onToggleSave: (Article) -> Unit = {},
        refreshWhen: Flow<Unit> = emptyFlow(),
    ) = compose.setContent {
        TodayFeedTheme {
            FeedScreen(
                articles =
                    flowOf(
                        PagingData.from(
                            data = articles,
                            sourceLoadStates =
                                LoadStates(
                                    refresh = refresh,
                                    prepend = LoadState.NotLoading(true),
                                    append = append
                                )
                        )
                    ),
                sections = sections,
                offline = offline,
                onArticleClick = onArticleClick,
                onRefreshSections = onRefreshSections,
                onToggleSave = onToggleSave,
                refreshWhen = refreshWhen
            )
        }
    }

    private companion object {
        const val APPENDING = "Loading more"
        const val ARTICLES = "Articles"
        const val OUT_OF_DATE = "You are offline. These articles may be out of date."

        val hero =
            FeedSection.WeatherHero(
                Weather(
                    placeName = "Taipei",
                    temperatureCelsius = 30.0,
                    condition = "Cloudy",
                    highCelsius = 31.0,
                    lowCelsius = 26.0
                )
            )

        fun article(
            id: String,
            title: String,
        ) = Article(
            id = id,
            title = title,
            summary = "Summary $id",
            source = "Spaceflight News",
            imageUrl = null,
            publishedAt = Instant.EPOCH
        )
    }
}
