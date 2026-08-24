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

    @Test
    fun `the first collector gets a reading from the source`() =
        runTest {
            val weather = repository.observeCurrent().first { it != null }

            assertEquals(25.5, weather?.temperatureCelsius ?: 0.0, 0.001)
            assertEquals(1, service.calls)
        }

    @Test
    fun `a second collector inside the allowance asks for nothing`() =
        runTest {
            repository.observeCurrent().first { it != null }

            clock.advanceBy(Duration.ofMinutes(5))
            repository.observeCurrent().first { it != null }

            assertEquals(1, service.calls)
        }

    @Test
    fun `past the allowance it asks again`() =
        runTest {
            repository.observeCurrent().first { it != null }

            clock.advanceBy(Duration.ofMinutes(16))
            repository.observeCurrent().first()

            assertEquals(2, service.calls)
        }

    /** No network and nothing held is a missing card, not a failure next to the articles. */
    @Test
    fun `with no connection and nothing held it asks for nothing and shows nothing`() =
        runTest {
            connectivity.set(Connection.Offline)

            assertNull(repository.observeCurrent().first())
            assertEquals(0, service.calls)
        }
}
