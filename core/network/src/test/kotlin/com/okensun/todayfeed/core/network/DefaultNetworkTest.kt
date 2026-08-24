package com.okensun.todayfeed.core.network

import com.okensun.todayfeed.core.freshness.Connection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Plain objects stand in for networks, because only their identity is ever used. */
class DefaultNetworkTest {
    private val wifi = Any()
    private val mobile = Any()
    private val default = DefaultNetwork()

    @Test
    fun `losing the network the answers were about is offline`() {
        default.changed(wifi, Connection.Unmetered)

        assertEquals(Connection.Offline, default.lost(wifi))
    }

    /**
     * Turning wifi off with mobile data on. The platform hands over the new network before it
     * reports the old one gone, so this loss is stale, and answering it would read as offline on
     * a phone that has a working connection.
     */
    @Test
    fun `losing wifi after mobile data took over says nothing`() {
        default.changed(wifi, Connection.Unmetered)
        default.changed(mobile, Connection.Metered)

        assertNull(default.lost(wifi))
    }

    @Test
    fun `a loss before any network was reported says nothing`() {
        assertNull(default.lost(wifi))
    }

    /** The network after a loss is reported in turn, or one handover would silence the rest. */
    @Test
    fun `the network that arrives after a loss is reported`() {
        default.changed(wifi, Connection.Unmetered)
        default.lost(wifi)

        assertEquals(Connection.Metered, default.changed(mobile, Connection.Metered))
        assertEquals(Connection.Offline, default.lost(mobile))
    }
}
