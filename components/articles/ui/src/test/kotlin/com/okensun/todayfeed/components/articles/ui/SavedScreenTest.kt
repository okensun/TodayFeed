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
class SavedScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `content lists what was saved`() {
        setState(ContentState.Content(listOf(article)))

        compose.onNodeWithText(article.title).assertIsDisplayed()
    }

    @Test
    fun `loading says so and lists nothing`() {
        setState(ContentState.Loading)

        compose.onNodeWithText("Loading").assertIsDisplayed()
        compose.onNodeWithText(article.title).assertDoesNotExist()
    }

    @Test
    fun `empty says nothing has been saved`() {
        setState(ContentState.Empty)

        compose.onNodeWithText("Nothing saved yet").assertIsDisplayed()
    }

    /** Offline and empty used to be the same picture on this screen. They must not be. */
    @Test
    fun `offline does not look like empty`() {
        setState(ContentState.Offline(null))

        compose.onNodeWithText("You are offline").assertIsDisplayed()
        compose.onNodeWithText("Nothing saved yet").assertDoesNotExist()
    }

    /** The retry on this screen used to be wired to an empty lambda. */
    @Test
    fun `error offers a retry that calls back`() {
        var retries = 0
        setState(ContentState.Error("Could not read what you saved."), onRetry = { retries++ })

        compose.onNodeWithText("Try again").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `offline with cached articles lists them and shows no offline notice`() {
        setState(ContentState.Offline(listOf(article)))

        compose.onNodeWithText(article.title).assertIsDisplayed()
        compose.onNodeWithText("You are offline").assertDoesNotExist()
    }

    @Test
    fun `tapping a saved article passes its id`() {
        var clicked: String? = null
        setState(ContentState.Content(listOf(article)), onArticleClick = { clicked = it })

        compose.onNodeWithText(article.title).performClick()

        assertEquals(article.id, clicked)
    }

    private fun setState(
        state: ContentState<List<Article>>,
        onRetry: () -> Unit = {},
        onArticleClick: (String) -> Unit = {},
    ) = compose.setContent {
        TodayFeedTheme {
            SavedScreen(state = state, onRetry = onRetry, onArticleClick = onArticleClick)
        }
    }

    private companion object {
        val article = previewArticle("s1")
    }
}
