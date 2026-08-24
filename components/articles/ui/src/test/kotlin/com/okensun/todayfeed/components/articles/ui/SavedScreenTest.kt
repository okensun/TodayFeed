package com.okensun.todayfeed.components.articles.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /** The tab reads in the order the repository gives it, which is most recently saved first. */
    @Test
    fun `the list keeps the order it was given`() {
        setState(
            ContentState.Content(
                listOf(article.copy(id = "newer", title = "Newer"), article.copy(id = "older", title = "Older"))
            )
        )

        val newer = compose.onNodeWithText("Newer").getBoundsInRoot()
        val older = compose.onNodeWithText("Older").getBoundsInRoot()

        assertTrue("newer at ${newer.top}, older at ${older.top}", newer.top < older.top)
    }

    @Test
    fun `unsaving from the tab passes the article back`() {
        var toggled: Article? = null
        setState(
            ContentState.Content(listOf(article.copy(saved = true))),
            onToggleSave = { toggled = it }
        )

        compose.onNodeWithContentDescription("Saved").performClick()

        assertEquals(article.id, toggled?.id)
    }

    private fun setState(
        state: ContentState<List<Article>>,
        onRetry: () -> Unit = {},
        onArticleClick: (String) -> Unit = {},
        onToggleSave: (Article) -> Unit = {},
    ) = compose.setContent {
        TodayFeedTheme {
            SavedScreen(
                state = state,
                onRetry = onRetry,
                onArticleClick = onArticleClick,
                onToggleSave = onToggleSave
            )
        }
    }

    private companion object {
        val article = previewArticle("s1")
    }
}
