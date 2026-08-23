package com.okensun.todayfeed.core.freshness

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FreshnessTest {
    @Test
    fun `nothing stored and offline leaves nothing to serve`() {
        assertEquals(
            Decision.NothingToServe,
            decide(cachedAt = null, connection = Connection.Offline)
        )
    }

    @Test
    fun `nothing stored with a network means the reader waits`() {
        assertEquals(Decision.Fetch, decide(cachedAt = null, connection = Connection.Unmetered))
    }

    @Test
    fun `offline serves what is stored, however old it is`() {
        assertEquals(
            Decision.ServeCacheStale,
            decide(cachedAt = NOW.minus(Duration.ofDays(7)), connection = Connection.Offline)
        )
    }

    @Test
    fun `young enough is served from the cache without asking`() {
        assertEquals(
            Decision.ServeCache,
            decide(cachedAt = NOW.minus(Duration.ofMinutes(4)))
        )
    }

    @Test
    fun `past its allowance is served and refreshed behind`() {
        assertEquals(
            Decision.ServeCacheThenFetch,
            decide(cachedAt = NOW.minus(Duration.ofMinutes(31)))
        )
    }

    @Test
    fun `a stated server age replaces our own figure`() {
        val age = Duration.ofMinutes(12)

        // Our figure would still call this fresh. The source says ten minutes, so it is not.
        assertEquals(
            Decision.ServeCacheThenFetch,
            decide(cachedAt = NOW.minus(age), serverMaxAge = Duration.ofMinutes(10))
        )
        assertEquals(Decision.ServeCache, decide(cachedAt = NOW.minus(age)))
    }

    @Test
    fun `the allowance does not depend on the connection`() {
        val past = NOW.minus(Duration.ofMinutes(31))

        assertEquals(
            decide(cachedAt = past, connection = Connection.Unmetered),
            decide(cachedAt = past, connection = Connection.Metered)
        )
    }

    @Test
    fun `exactly at the allowance is still fresh`() {
        assertEquals(Decision.ServeCache, decide(cachedAt = NOW.minus(TTL)))
    }

    @Test
    fun `one second before the allowance is fresh`() {
        assertEquals(
            Decision.ServeCache,
            decide(cachedAt = NOW.minus(TTL).plusSeconds(1))
        )
    }

    @Test
    fun `one second past the allowance is not`() {
        assertEquals(
            Decision.ServeCacheThenFetch,
            decide(cachedAt = NOW.minus(TTL).minusSeconds(1))
        )
    }

    @Test
    fun `content stored in the future is treated as fresh rather than as an error`() {
        assertEquals(
            Decision.ServeCache,
            decide(cachedAt = NOW.plus(Duration.ofMinutes(5)))
        )
    }

    private fun decide(
        cachedAt: Instant?,
        serverMaxAge: Duration? = null,
        connection: Connection = Connection.Unmetered,
    ): Decision =
        decide(
            cachedAt = cachedAt,
            serverMaxAge = serverMaxAge,
            timeToLive = TTL,
            connection = connection,
            now = NOW
        )

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-23T12:00:00Z")
        val TTL: Duration = Duration.ofMinutes(30)
    }
}
