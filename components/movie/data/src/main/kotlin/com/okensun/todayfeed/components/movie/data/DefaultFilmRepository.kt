package com.okensun.todayfeed.components.movie.data

import com.okensun.todayfeed.components.movie.api.FilmRepository
import com.okensun.todayfeed.components.movie.api.models.Film
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
 * Held in memory, like the weather. A catalogue of 32 KB that gains a film every few years does not
 * earn a database, and a missing section costs the reader nothing.
 */
@Singleton
internal class DefaultFilmRepository
    @Inject
    constructor(
        private val service: FilmService,
        private val connectivity: Connectivity,
        private val clock: Clock,
    ) : FilmRepository {
        private val films = MutableStateFlow(emptyList<Film>())

        private val asking = Mutex()
        private var fetchedAt: Instant? = null
        private var serverMaxAge: Duration? = null

        override fun observeFilms(): Flow<List<Film>> = films.asStateFlow()

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

        /** A failure leaves the row as it was and stamps nothing, so the next ask tries again. */
        @Suppress("SwallowedException")
        private suspend fun fetch() {
            try {
                val response = service.films()
                val body = response.body()
                if (!response.isSuccessful || body.isNullOrEmpty()) return
                films.value = body.map { it.toFilm() }.bestFirst()
                // The source says `no-cache`, which states no age at all, so ours is what holds.
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

        /** Title breaks a tie, so the same answer always draws the row in the same order. */
        private fun List<Film>.bestFirst(): List<Film> =
            sortedWith(compareByDescending<Film> { it.score ?: Int.MIN_VALUE }.thenBy { it.title })

        internal companion object {
            const val CACHE_CONTROL = "Cache-Control"

            /**
             * Half a day. Every film carries a review score, and a score moves, so the catalogue
             * gets an ordinary allowance like every other source. See DECISIONS.md.
             */
            val TIME_TO_LIVE: Duration = Duration.ofHours(12)
        }
    }
