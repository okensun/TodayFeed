package com.okensun.todayfeed.components.feed.domain

import app.cash.turbine.test
import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.testing.FakeConnectivity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveOfflineTest {
    @Test
    fun `a metered connection is not offline`() =
        runTest {
            val connectivity = FakeConnectivity(Connection.Metered)

            ObserveOffline(connectivity)().test {
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `losing and regaining the network is reported both times`() =
        runTest {
            val connectivity = FakeConnectivity(Connection.Unmetered)

            ObserveOffline(connectivity)().test {
                assertEquals(false, awaitItem())

                connectivity.set(Connection.Offline)
                assertEquals(true, awaitItem())

                connectivity.set(Connection.Unmetered)
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
