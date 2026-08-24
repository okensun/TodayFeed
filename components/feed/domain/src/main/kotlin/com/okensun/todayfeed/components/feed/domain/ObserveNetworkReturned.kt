package com.okensun.todayfeed.components.feed.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

/**
 * Emits each time the connection comes back, and never on the first answer.
 *
 * A reader who opened the app with no network has an empty screen and no second chance:
 * `initialize()` runs once per pager, so without this nothing asks again until the process
 * restarts. Clearing the marker is the visible half; this is the half that matters.
 */
class ObserveNetworkReturned
    @Inject
    constructor(
        private val observeOffline: ObserveOffline,
    ) {
        operator fun invoke(): Flow<Unit> =
            observeOffline()
                .distinctUntilChanged()
                .scan(Change(was = false, now = false)) { last, offline -> Change(last.now, offline) }
                .filter { it.was && !it.now }
                .map { }

        private data class Change(
            val was: Boolean,
            val now: Boolean,
        )
    }
