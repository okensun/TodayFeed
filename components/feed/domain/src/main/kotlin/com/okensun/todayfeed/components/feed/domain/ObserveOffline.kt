package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.freshness.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Whether the screen is reading what it already has. A `ui` module may not name [Connection], so
 * the answer crosses the boundary as a plain flag rather than as the connection itself.
 */
class ObserveOffline
    @Inject
    constructor(
        private val connectivity: Connectivity,
    ) {
        operator fun invoke(): Flow<Boolean> = connectivity.observe().map { it == Connection.Offline }
    }
