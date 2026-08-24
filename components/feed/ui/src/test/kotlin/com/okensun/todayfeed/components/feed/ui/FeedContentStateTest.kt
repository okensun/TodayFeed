package com.okensun.todayfeed.components.feed.ui

import androidx.paging.LoadState
import com.okensun.todayfeed.core.designsystem.ContentState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    /**
     * This asserted the opposite, which locked a bug in place. The weather source always has a
     * value, so treating a section as content ahead of a failure made the error state and its
     * retry unreachable in the real app: an article source that was down showed a weather card, an
     * empty list, and no way to try again.
     */
    @Test
    fun `a failure with no articles is an error even when a section has content`() {
        assertTrue(
            state(LoadState.Error(RuntimeException("x")), 0, hasSections = true)
                is ContentState.Error
        )
    }

    /** Offline is not a failure. Articles in hand stay on screen, marked as what they are. */
    @Test
    fun `offline with articles is offline carrying them, not an error`() {
        val result = state(done, itemCount = 5, offline = true)

        assertTrue(result is ContentState.Offline)
        assertNotNull((result as ContentState.Offline).cached)
    }

    @Test
    fun `offline with only a section still carries it`() {
        val result = state(done, itemCount = 0, hasSections = true, offline = true)

        assertNotNull((result as ContentState.Offline).cached)
    }

    /** The dead end: offline and nothing stored. Even this offers a retry rather than nothing. */
    @Test
    fun `offline with nothing at all carries nothing`() {
        val result = state(done, itemCount = 0, offline = true)

        assertTrue(result is ContentState.Offline)
        assertNull((result as ContentState.Offline).cached)
    }

    /**
     * Offline is settled after a failure that has nothing to show, so the retry stays reachable.
     * Ordering, again, rather than a rule anyone has to remember.
     */
    @Test
    fun `offline with nothing loaded and a failure is still the error`() {
        val result = state(LoadState.Error(RuntimeException("x")), itemCount = 0, offline = true)

        assertTrue(result is ContentState.Error)
    }

    private fun state(
        refresh: LoadState,
        itemCount: Int,
        hasSections: Boolean = false,
        offline: Boolean = false,
    ) = feedContentState(refresh, itemCount, hasSections, offline)

    private companion object {
        val done = LoadState.NotLoading(endOfPaginationReached = true)
    }
}
