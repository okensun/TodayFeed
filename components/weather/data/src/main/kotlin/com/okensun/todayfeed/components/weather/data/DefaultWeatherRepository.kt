package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.components.weather.api.WeatherRepository
import com.okensun.todayfeed.core.freshness.Connectivity
import com.okensun.todayfeed.core.freshness.decide
import com.okensun.todayfeed.core.freshness.wantsNetwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Held in memory rather than in a database. Weather is worth minutes, and a card that is missing
 * on a cold start with no network is better than one showing yesterday as if it were now.
 */
@Singleton
internal class DefaultWeatherRepository
    @Inject
    constructor(
        private val service: WeatherService,
        private val connectivity: Connectivity,
        private val clock: Clock,
    ) : WeatherRepository {
        private val current = MutableStateFlow<Weather?>(null)
        private var fetchedAt: Instant? = null

        override fun observeCurrent(): Flow<Weather?> = current.asStateFlow().onStart { refresh() }

        private suspend fun refresh() {
            val decision =
                decide(
                    cachedAt = fetchedAt,
                    // The source states no maximum age of its own, so ours is the one that counts.
                    serverMaxAge = null,
                    timeToLive = TIME_TO_LIVE,
                    connection = connectivity.current(),
                    now = clock.instant()
                )
            if (!decision.wantsNetwork) return
            fetch()
        }

        /**
         * A failure leaves the card as it was. There is no error state for weather: the feed is
         * about articles, and a missing card says less wrong than an error next to them.
         */
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun fetch() {
            try {
                val response = service.forecast(latitude = LATITUDE, longitude = LONGITUDE)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    current.value = body.toWeather(PLACE)
                    fetchedAt = clock.instant()
                }
            } catch (cancelled: CancellationException) {
                @Suppress("RethrowCaughtException")
                throw cancelled
            } catch (failed: Throwable) {
                // Deliberate: see the note above.
            }
        }

        internal companion object {
            /** The place is fixed, so the app asks for no location permission. */
            const val PLACE = "Taipei"
            const val LATITUDE = 25.033
            const val LONGITUDE = 121.5654

            /**
             * The source refreshes its own reading every 900 seconds, which it reports as
             * `interval` in the response. Asking more often than it changes buys nothing.
             */
            val TIME_TO_LIVE: Duration = Duration.ofMinutes(15)
        }
    }
