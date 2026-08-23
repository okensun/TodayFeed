package com.okensun.todayfeed.core.designsystem

/**
 * The four states every content area must be able to show, plus the loaded case.
 *
 * [Offline] is not a kind of [Error] on purpose. In an offline-first app, being offline
 * while holding cached content is normal and is not a failure, so [Offline] carries the
 * content it has. Folding it into [Error] would force a screen to choose between showing
 * cached content and admitting it may be old. See DECISIONS.md.
 */
sealed interface ContentState<out T> {

    data object Loading : ContentState<Nothing>

    data object Empty : ContentState<Nothing>

    data class Error(val message: String) : ContentState<Nothing>

    data class Offline<out T>(val cached: T?) : ContentState<T>

    data class Content<out T>(val value: T) : ContentState<T>
}
