package com.okensun.todayfeed.components.feed.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.feed.domain.FeedSection
import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import kotlinx.coroutines.flow.flowOf
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

    private fun show(
        articles: List<Article>,
        sections: List<FeedSection> = emptyList(),
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        onArticleClick: (String) -> Unit = {},
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
                onArticleClick = onArticleClick
            )
        }
    }

    private companion object {
        const val APPENDING = "Loading more"

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
