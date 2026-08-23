package com.okensun.todayfeed.core.testing

import app.cash.turbine.test
import com.okensun.todayfeed.core.freshness.Connection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeConnectivityTest {
    @Test
    fun `reports what it was set to`() {
        val connectivity = FakeConnectivity(Connection.Metered)

        assertEquals(Connection.Metered, connectivity.current())
    }

    @Test
    fun `emits every change`() =
        runTest {
            val connectivity = FakeConnectivity(Connection.Unmetered)

            connectivity.observe().test {
                assertEquals(Connection.Unmetered, awaitItem())

                connectivity.set(Connection.Offline)
                assertEquals(Connection.Offline, awaitItem())

                connectivity.set(Connection.Metered)
                assertEquals(Connection.Metered, awaitItem())
            }
        }
}
