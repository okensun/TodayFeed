package com.okensun.todayfeed.components.articles.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.NO_PICTURE
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleRowCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `an article with no picture still shows its title and source`() {
        show(previewArticle("a1").copy(imageUrl = null))

        compose.onNodeWithText(TITLE).assertIsDisplayed()
        compose.onNodeWithText("European Spaceflight").assertIsDisplayed()
    }

    /**
     * A picture that cannot be fetched says so and stops waiting. This runs with no network
     * fetcher on the classpath, which is the same answer a timeout or a refusal gives.
     */
    @Test
    fun `a picture that cannot be fetched shows the no picture mark and keeps the title`() {
        show(previewArticle("a2").copy(imageUrl = "https://example.invalid/missing.jpg"))

        compose.onNodeWithText(TITLE).assertIsDisplayed()
        compose.onNodeWithContentDescription(NO_PICTURE).assertIsDisplayed()
    }

    /** An article that never had one is not a failure, so it is not marked as one. */
    @Test
    fun `an article with no picture at all shows no mark`() {
        show(previewArticle("a6").copy(imageUrl = null))

        compose.onNodeWithContentDescription(NO_PICTURE).assertDoesNotExist()
    }

    @Test
    fun `an unsaved article shows the hollow star`() {
        show(previewArticle("a3").copy(saved = false))

        compose.onNodeWithContentDescription("Save").assertIsDisplayed()
    }

    @Test
    fun `a saved article shows the filled star`() {
        show(previewArticle("a4").copy(saved = true))

        compose.onNodeWithContentDescription("Saved").assertIsDisplayed()
    }

    @Test
    fun `the star calls back once and the card calls back on its own tap`() {
        var toggles = 0
        var clicks = 0
        show(previewArticle("a5"), onClick = { clicks++ }, onToggleSave = { toggles++ })

        compose.onNodeWithContentDescription("Save").performClick()
        compose.onNodeWithText(TITLE).performClick()

        assertEquals(1, toggles)
        assertEquals(1, clicks)
    }

    private fun show(
        article: Article,
        onClick: () -> Unit = {},
        onToggleSave: () -> Unit = {},
    ) = compose.setContent {
        TodayFeedTheme {
            ArticleRowCard(article = article, onClick = onClick, onToggleSave = onToggleSave)
        }
    }

    private companion object {
        const val TITLE = "CNES seeks partners to mass produce compact optical telescopes"
    }
}
