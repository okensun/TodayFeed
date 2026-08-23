package com.okensun.todayfeed.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * A clock that only moves when a test tells it to.
 *
 * This is what lets a freshness test read as a sequence of events: write an entry, advance
 * forty minutes, assert that the next read wants a refresh. With a real clock the same test
 * would need arithmetic on timestamps, or a sleep.
 */
class FakeClock(
    private var current: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    private val zone: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    override fun instant(): Instant = current

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = FakeClock(current, zone)

    fun advanceBy(duration: Duration) {
        require(!duration.isNegative) { "A clock cannot run backwards: $duration" }
        current = current.plus(duration)
    }

    fun setTo(instant: Instant) {
        current = instant
    }
}
