package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.testing.FakeClock
import com.okensun.todayfeed.core.testing.FakeConnectivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class DefaultWeatherRepositoryTest {
    private val service = FakeWeatherService()
    private val clock = FakeClock()
    private val connectivity = FakeConnectivity()

    private val repository = DefaultWeatherRepository(service, connectivity, clock)

    /**
     * Collecting is not asking. Something has to say when — the screen opening, a pull, or the
     * network returning — or a reader who was offline at the start would never see the card.
     */
    @Test
    fun `collecting on its own asks for nothing`() =
        runTest {
            assertNull(repository.observeCurrent().first())

            assertEquals(0, service.calls)
        }

    @Test
    fun `a refresh brings a reading`() =
        runTest {
            repository.refresh()

            val weather = repository.observeCurrent().first()
            assertEquals(25.5, weather?.temperatureCelsius ?: 0.0, 0.001)
            assertEquals(1, service.calls)
        }

    @Test
    fun `a refresh inside the allowance asks for nothing`() =
        runTest {
            repository.refresh()

            clock.advanceBy(Duration.ofMinutes(5))
            repository.refresh()

            assertEquals(1, service.calls)
        }

    @Test
    fun `past the allowance it asks again`() =
        runTest {
            repository.refresh()

            clock.advanceBy(Duration.ofMinutes(16))
            repository.refresh()

            assertEquals(2, service.calls)
        }

    /** No network and nothing held is a missing card, not a failure next to the articles. */
    @Test
    fun `with no connection it asks for nothing and shows nothing`() =
        runTest {
            connectivity.set(Connection.Offline)

            repository.refresh()

            assertNull(repository.observeCurrent().first())
            assertEquals(0, service.calls)
        }

    @Test
    fun `a failure leaves the reading that was already held`() =
        runTest {
            repository.refresh()

            service.failure = FakeWeatherService.Failure.NoNetwork
            clock.advanceBy(Duration.ofMinutes(16))
            repository.refresh()

            assertEquals(25.5, repository.observeCurrent().first()?.temperatureCelsius ?: 0.0, 0.001)
        }

    /** A failure stamps nothing, so the next ask tries rather than waiting out the allowance. */
    @Test
    fun `a failure does not buy silence`() =
        runTest {
            service.failure = FakeWeatherService.Failure.ServerError
            repository.refresh()

            service.failure = null
            repository.refresh()

            assertEquals(2, service.calls)
            assertEquals(25.5, repository.observeCurrent().first()?.temperatureCelsius ?: 0.0, 0.001)
        }
}
