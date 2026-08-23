package com.okensun.todayfeed.components.feed.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.feed.domain.FeedItem
import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.core.designsystem.ContentState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * View tests: given a state, does the screen draw the right thing and do its callbacks fire.
 * They run on the JVM through Robolectric, so no device is involved.
 */
@RunWith(RobolectricTestRunner::class)
class FeedScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `content shows the weather hero above the article titles`() {
        setState(ContentState.Content(feed))

        compose.onNodeWithText("Taipei").assertIsDisplayed()
        compose.onNodeWithText("Article one").assertIsDisplayed()
    }

    @Test
    fun `empty says there is nothing to read`() {
        setState(ContentState.Empty)

        compose.onNodeWithText("Nothing to read yet").assertIsDisplayed()
    }

    @Test
    fun `error offers a retry that calls back`() {
        var retries = 0
        setState(ContentState.Error("Could not reach the server."), onRetry = { retries++ })

        compose.onNodeWithText("Could not reach the server.").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `offline with nothing cached offers a retry that calls back`() {
        var retries = 0
        setState(ContentState.Offline(null), onRetry = { retries++ })

        compose.onNodeWithText("You are offline").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `offline with cached content shows the content, not an error`() {
        setState(ContentState.Offline(feed))

        compose.onNodeWithText("Article one").assertIsDisplayed()
        compose.onNodeWithText("Taipei").assertIsDisplayed()
    }

    @Test
    fun `tapping an article passes its id`() {
        var clicked: String? = null
        setState(ContentState.Content(feed), onArticleClick = { clicked = it })

        compose.onNodeWithText("Article one").performClick()

        assertEquals("a1", clicked)
    }

    private fun setState(
        state: ContentState<List<FeedItem>>,
        onRetry: () -> Unit = {},
        onArticleClick: (String) -> Unit = {},
    ) = compose.setContent {
        FeedScreen(state = state, onRetry = onRetry, onArticleClick = onArticleClick)
    }

    private companion object {
        val feed =
            listOf(
                FeedItem.WeatherHero(
                    Weather(
                        placeName = "Taipei",
                        temperatureCelsius = 30.0,
                        condition = "Cloudy",
                        highCelsius = 31.0,
                        lowCelsius = 26.0
                    )
                ),
                FeedItem.ArticleRow(
                    Article(
                        id = "a1",
                        title = "Article one",
                        summary = "Summary one",
                        source = "Spaceflight News",
                        imageUrl = null,
                        publishedAt = Instant.EPOCH
                    )
                )
            )
    }
}
