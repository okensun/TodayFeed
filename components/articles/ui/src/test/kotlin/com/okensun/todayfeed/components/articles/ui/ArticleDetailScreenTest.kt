package com.okensun.todayfeed.components.articles.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
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
        val article = previewArticle()
        setState(ContentState.Content(article))

        compose.onNodeWithText(article.title).assertIsDisplayed()
        compose.onNodeWithText(article.source).assertIsDisplayed()
        compose.onNodeWithText(article.summary).assertIsDisplayed()
    }

    /**
     * The screen used to fold Offline into its error branch, so an article that was cached
     * but not saved was reported as missing while the state was holding it.
     */
    @Test
    fun `offline with a cached article shows the article, not a missing message`() {
        val article = previewArticle()
        setState(ContentState.Offline(article))

        compose.onNodeWithText(article.title).assertIsDisplayed()
        compose.onNodeWithText("That article could not be found.").assertIsNotDisplayed()
    }

    @Test
    fun `offline with nothing cached says the device is offline`() {
        setState(ContentState.Offline(null))

        compose.onNodeWithText("You are offline").assertIsDisplayed()
    }

    @Test
    fun `empty reports the article as missing`() {
        setState(ContentState.Empty)

        compose.onNodeWithText("That article could not be found.").assertIsDisplayed()
    }

    @Test
    fun `leaving on an error calls back`() {
        var backs = 0
        setState(ContentState.Error("That article could not be found."), onBack = { backs++ })

        compose.onNodeWithText("Try again").performClick()

        assertEquals(1, backs)
    }

    private fun setState(
        state: ContentState<Article>,
        onBack: () -> Unit = {},
    ) = compose.setContent {
        ArticleDetailScreen(state = state, onBack = onBack)
    }
}
