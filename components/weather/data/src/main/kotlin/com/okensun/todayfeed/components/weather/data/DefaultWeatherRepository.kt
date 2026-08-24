package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.components.weather.api.WeatherRepository
import com.okensun.todayfeed.components.weather.api.models.Weather
import com.okensun.todayfeed.core.freshness.Connectivity
import com.okensun.todayfeed.core.freshness.decide
import com.okensun.todayfeed.core.freshness.wantsNetwork
import com.okensun.todayfeed.core.network.maxAgeOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.IOException
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

        // Everything the decision reads is written under this, so two collectors arriving inside
        // one round trip make one request rather than two.
        private val asking = Mutex()
        private var fetchedAt: Instant? = null
        private var serverMaxAge: Duration? = null

        /** Collecting never fetches. What is held is emitted at once, whatever its age. */
        override fun observeCurrent(): Flow<Weather?> = current.asStateFlow()

        override suspend fun refresh() {
            asking.withLock {
                val decision =
                    decide(
                        cachedAt = fetchedAt,
                        serverMaxAge = serverMaxAge,
                        timeToLive = TIME_TO_LIVE,
                        connection = connectivity.current(),
                        now = clock.instant()
                    )
                if (decision.wantsNetwork) fetch()
            }
        }

        /**
         * A failure leaves the card as it was and stamps nothing, so the next ask tries again
         * rather than waiting out the allowance. There is no error state for weather: the feed is
         * about articles, and a missing card says less wrong than an error beside them.
         */
        @Suppress("SwallowedException")
        private suspend fun fetch() {
            try {
                val response = service.forecast(latitude = LATITUDE, longitude = LONGITUDE)
                val body = response.body()
                if (!response.isSuccessful || body == null) return
                current.value = body.toWeather(PLACE)
                // If the source ever starts stating an age, it wins over ours. It states none today.
                serverMaxAge = maxAgeOf(response.headers().values(CACHE_CONTROL).joinToString(", "))
                fetchedAt = clock.instant()
            } catch (cancelled: CancellationException) {
                @Suppress("RethrowCaughtException")
                throw cancelled
            } catch (offline: IOException) {
                return
            } catch (changed: SerializationException) {
                return
            }
        }

        internal companion object {
            /** The place is fixed, so the app asks for no location permission. */
            const val PLACE = "Taipei"
            const val LATITUDE = 25.033
            const val LONGITUDE = 121.5654
            const val CACHE_CONTROL = "Cache-Control"

            /**
             * The source re-reads every 900 seconds, which it reports as `interval` in its own
             * answer. Asking more often than it changes buys nothing.
             */
            val TIME_TO_LIVE: Duration = Duration.ofMinutes(15)
        }
    }
