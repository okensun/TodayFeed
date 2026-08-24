package com.okensun.todayfeed.components.feed.domain

import app.cash.turbine.test
import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.testing.FakeConnectivity
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveNetworkReturnedTest {
    @Test
    fun `starting online is not a return`() =
        runTest {
            returned(Connection.Unmetered).test {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** The case this exists for: opened with no network, then the network arrives. */
    @Test
    fun `starting offline and then connecting is a return`() =
        runTest {
            val connectivity = FakeConnectivity(Connection.Offline)

            ObserveNetworkReturned(ObserveOffline(connectivity))().test {
                expectNoEvents()

                connectivity.set(Connection.Unmetered)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `moving between metered and unmetered is not a return`() =
        runTest {
            val connectivity = FakeConnectivity(Connection.Unmetered)

            ObserveNetworkReturned(ObserveOffline(connectivity))().test {
                connectivity.set(Connection.Metered)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `losing and regaining twice returns twice`() =
        runTest {
            val connectivity = FakeConnectivity(Connection.Unmetered)

            ObserveNetworkReturned(ObserveOffline(connectivity))().test {
                connectivity.set(Connection.Offline)
                connectivity.set(Connection.Unmetered)
                awaitItem()

                connectivity.set(Connection.Offline)
                connectivity.set(Connection.Metered)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun returned(connection: Connection) = ObserveNetworkReturned(ObserveOffline(FakeConnectivity(connection)))()
}
