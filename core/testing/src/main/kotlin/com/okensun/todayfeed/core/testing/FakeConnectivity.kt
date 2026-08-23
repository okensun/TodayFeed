package com.okensun.todayfeed.core.testing

import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.freshness.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A connection a test sets directly. */
class FakeConnectivity(
    initial: Connection = Connection.Unmetered,
) : Connectivity {
    private val connection = MutableStateFlow(initial)

    override fun current(): Connection = connection.value

    override fun observe(): Flow<Connection> = connection.asStateFlow()

    fun set(connection: Connection) {
        this.connection.value = connection
    }
}
