package com.okensun.todayfeed.components.feed.ui

import androidx.paging.LoadState
import com.okensun.todayfeed.core.designsystem.ContentState

/**
 * Picks which of the four states the screen shows. The content itself comes from the paged stream
 * and the sections, so the payload here is [Unit]: this decides the shape, not what is in it.
 *
 * Anything already on screen wins, which is how "a failed refresh must not empty the screen" is
 * expressed. It is the order of the conditions rather than a rule anyone has to remember.
 */
internal fun feedContentState(
    refresh: LoadState,
    itemCount: Int,
    hasSections: Boolean,
): ContentState<Unit> =
    when {
        itemCount > 0 || hasSections -> ContentState.Content(Unit)
        refresh is LoadState.Loading -> ContentState.Loading
        refresh is LoadState.Error ->
            ContentState.Error(
                refresh.error.message ?: "The feed could not be loaded."
            )
        else -> ContentState.Empty
    }
