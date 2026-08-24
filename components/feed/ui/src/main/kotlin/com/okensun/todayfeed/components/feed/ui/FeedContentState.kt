package com.okensun.todayfeed.components.feed.ui

import androidx.paging.LoadState
import com.okensun.todayfeed.core.designsystem.ContentState

/**
 * Picks which of the four states the screen shows. The content itself comes from the paged stream
 * and the sections, so the payload here is [Unit]: this decides the shape, not what is in it.
 *
 * The order matters and each step earns its place:
 *
 * 1. Articles already on screen win, which is how "a failed refresh must not empty the screen" is
 *    expressed. It is the order of the conditions rather than a rule anyone has to remember.
 * 2. A failure with no articles is an error, **even when a section has something to show**. An
 *    earlier version put sections above this, which made the error state and its retry
 *    unreachable whenever a section had anything: an article source that was down showed a
 *    weather card, an empty list, and no way to try again.
 * 3. Only then do sections count as content, which keeps a weather card from being replaced by
 *    "nothing to read" when there is simply nothing to read yet.
 * 4. Offline is settled last of all, and only decides how content is labelled. It is not a
 *    failure, so it never takes an error's place and never hides what is held.
 */
internal fun feedContentState(
    refresh: LoadState,
    itemCount: Int,
    hasSections: Boolean,
    offline: Boolean,
): ContentState<Unit> =
    when {
        itemCount > 0 -> held(offline)
        refresh is LoadState.Error ->
            ContentState.Error(
                refresh.error.message ?: "The feed could not be loaded."
            )
        refresh is LoadState.Loading -> ContentState.Loading
        hasSections -> held(offline)
        offline -> ContentState.Offline(null)
        else -> ContentState.Empty
    }

/** Offline is not a failure: the same content, marked as what it is. */
private fun held(offline: Boolean): ContentState<Unit> = if (offline) ContentState.Offline(Unit) else ContentState.Content(Unit)
