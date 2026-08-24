package com.okensun.todayfeed.core.network

import com.okensun.todayfeed.core.freshness.Connection
import org.junit.Assert.assertEquals
import org.junit.Test

/** The rules are a plain function, so this needs no device and no Robolectric. */
class ConnectionOfTest {
    @Test
    fun `nothing validated is offline, however the network describes itself`() {
        assertEquals(Connection.Offline, connectionOf(validated = false, notMetered = true))
        assertEquals(Connection.Offline, connectionOf(validated = false, notMetered = false))
    }

    @Test
    fun `a validated network that is not metered is unmetered`() {
        assertEquals(Connection.Unmetered, connectionOf(validated = true, notMetered = true))
    }

    /** Wifi shared from a phone says it is metered, and that is the answer we want. */
    @Test
    fun `a validated network that is metered is metered`() {
        assertEquals(Connection.Metered, connectionOf(validated = true, notMetered = false))
    }
}
