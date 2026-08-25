package com.okensun.todayfeed.components.movie.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.okensun.todayfeed.components.movie.api.models.Film
import com.okensun.todayfeed.core.designsystem.NO_PICTURE
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilmCarouselCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `each film shows its title, its year and its score`() {
        show(listOf(film("1", "Castle in the Sky", "1986"), film("2", "Kiki", "1989")))

        compose.onNodeWithText("Castle in the Sky").assertIsDisplayed()
        compose.onNodeWithText("1986  ·  95%").assertIsDisplayed()
        compose.onNodeWithText("Kiki").assertIsDisplayed()
    }

    /** No score is a missing word, not a nought that would read as the worst film ever made. */
    @Test
    fun `a film with no score shows its year alone`() {
        show(listOf(film("1", "Castle in the Sky", "1986", score = null)))

        compose.onNodeWithText("1986").assertIsDisplayed()
    }

    /** The picture is the part that can fail. The words are the part that must not. */
    @Test
    fun `a film with no picture still shows its title`() {
        show(listOf(film("1", "Castle in the Sky", "1986", banner = null)))

        compose.onNodeWithText("Castle in the Sky").assertIsDisplayed()
    }

    /**
     * Side by side, a card that drew no banner would be shorter than the rest of the row. The
     * space is held, so the cards line up whether the picture exists or not.
     */
    @Test
    fun `a film with no picture is the same height as one with a picture`() {
        show(listOf(film("1", "Castle in the Sky", "1986", banner = null), film("2", "Kiki", "1989")))

        val without = compose.onNodeWithText("Castle in the Sky").getBoundsInRoot()
        val with = compose.onNodeWithText("Kiki").getBoundsInRoot()

        assertEquals(with.top, without.top)
    }

    @Test
    fun `a film with no picture says so where the picture would be`() {
        show(listOf(film("1", "Castle in the Sky", "1986", banner = null)))

        compose.onNodeWithContentDescription(NO_PICTURE).assertIsDisplayed()
    }

    /** Nothing to show is not a row with nothing in it. */
    @Test
    fun `no films draws nothing at all`() {
        show(emptyList())

        compose.onNodeWithText("Films").assertDoesNotExist()
    }

    private fun show(films: List<Film>) = compose.setContent { TodayFeedTheme { FilmCarouselCard(films = films) } }

    private fun film(
        id: String,
        title: String,
        year: String,
        banner: String? = "https://example.test/$id.jpg",
        score: Int? = 95,
    ) = Film(
        id = id,
        title = title,
        year = year,
        director = "Hayao Miyazaki",
        bannerUrl = banner,
        score = score
    )
}
