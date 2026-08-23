package com.okensun.todayfeed.components.articles.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `content shows the title, the source and the summary`() {
        setState(ContentState.Content(article))

        compose.onNodeWithText(article.title).assertIsDisplayed()
        compose.onNodeWithText(article.source).assertIsDisplayed()
        compose.onNodeWithText(article.summary).assertIsDisplayed()
    }

    @Test
    fun `loading says so and shows no article`() {
        setState(ContentState.Loading)

        compose.onNodeWithText("Loading").assertIsDisplayed()
        compose.onNodeWithText(article.title).assertDoesNotExist()
    }

    /**
     * The screen used to fold Offline into its error branch, so an article that was cached
     * but not saved was reported as missing while the state was holding it.
     */
    @Test
    fun `offline with a cached article shows the article, not a missing message`() {
        setState(ContentState.Offline(article))

        compose.onNodeWithText(article.title).assertIsDisplayed()
        compose.onNodeWithText(NOT_FOUND).assertDoesNotExist()
        compose.onNodeWithText("You are offline").assertDoesNotExist()
    }

    @Test
    fun `offline with nothing cached offers a way back that calls back`() {
        var backs = 0
        setState(ContentState.Offline(null), onBack = { backs++ })

        compose.onNodeWithText("You are offline").assertIsDisplayed()
        compose.onNodeWithText("Go back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `empty reports the article as missing`() {
        setState(ContentState.Empty)

        compose.onNodeWithText(NOT_FOUND).assertIsDisplayed()
    }

    /**
     * A distinct message from the empty branch, so this test cannot pass by accident if the
     * two branches are ever swapped. The button leaves the screen, so it does not say retry.
     */
    @Test
    fun `an error shows its own message and a way back that calls back`() {
        var backs = 0
        setState(ContentState.Error("Could not reach the server."), onBack = { backs++ })

        compose.onNodeWithText("Could not reach the server.").assertIsDisplayed()
        compose.onNodeWithText(NOT_FOUND).assertDoesNotExist()
        compose.onNodeWithText("Go back").performClick()

        assertEquals(1, backs)
    }

    private fun setState(
        state: ContentState<Article>,
        onBack: () -> Unit = {},
    ) = compose.setContent {
        TodayFeedTheme {
            ArticleDetailScreen(state = state, onBack = onBack)
        }
    }

    private companion object {
        const val NOT_FOUND = "That article could not be found."
        val article = previewArticle("d1")
    }
}
