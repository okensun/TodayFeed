package com.okensun.todayfeed.components.feed.ui

import androidx.paging.LoadState
import com.okensun.todayfeed.core.designsystem.ContentState
import org.junit.Assert.assertTrue
import org.junit.Test

/** A plain function, so a plain test. No Robolectric and no Compose involved. */
class FeedContentStateTest {
    @Test
    fun `nothing yet and still loading shows loading`() {
        assertTrue(state(LoadState.Loading, itemCount = 0) is ContentState.Loading)
    }

    @Test
    fun `nothing at all shows empty`() {
        assertTrue(state(done, itemCount = 0) is ContentState.Empty)
    }

    @Test
    fun `nothing and a failure shows the error`() {
        val result = state(LoadState.Error(RuntimeException("no network")), itemCount = 0)

        assertTrue(result is ContentState.Error)
        assertTrue((result as ContentState.Error).message.contains("no network"))
    }

    @Test
    fun `an error without a message still says something`() {
        val result = state(LoadState.Error(RuntimeException()), itemCount = 0)

        assertTrue((result as ContentState.Error).message.isNotBlank())
    }

    /** A failed refresh must not empty the screen. This is the condition order, not a rule. */
    @Test
    fun `articles already loaded survive a failure`() {
        assertTrue(
            state(LoadState.Error(RuntimeException("x")), itemCount = 5) is ContentState.Content
        )
    }

    @Test
    fun `articles already loaded survive a reload`() {
        assertTrue(state(LoadState.Loading, itemCount = 5) is ContentState.Content)
    }

    /** A weather card with no articles shows the weather card, not "nothing to read". */
    @Test
    fun `a section with no articles is content, not empty`() {
        assertTrue(state(done, itemCount = 0, hasSections = true) is ContentState.Content)
    }

    @Test
    fun `a section also survives a failure`() {
        assertTrue(
            state(LoadState.Error(RuntimeException("x")), 0, hasSections = true)
                is ContentState.Content
        )
    }

    private fun state(
        refresh: LoadState,
        itemCount: Int,
        hasSections: Boolean = false,
    ) = feedContentState(refresh, itemCount, hasSections)

    private companion object {
        val done = LoadState.NotLoading(endOfPaginationReached = true)
    }
}
