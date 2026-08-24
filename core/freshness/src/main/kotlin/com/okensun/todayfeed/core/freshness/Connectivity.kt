package com.okensun.todayfeed.core.freshness

import kotlinx.coroutines.flow.Flow

/**
 * Reports what a byte costs right now. An interface so that this module stays plain Kotlin and a
 * test can answer for it in one line.
 */
interface Connectivity {
    fun current(): Connection

    /** Emits on every change, so a screen can drop an out-of-date marker when the network returns. */
    fun observe(): Flow<Connection>
}
