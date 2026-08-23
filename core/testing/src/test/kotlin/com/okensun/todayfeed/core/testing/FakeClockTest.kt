package com.okensun.todayfeed.core.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FakeClockTest {
    @Test
    fun `stays still until it is advanced`() {
        val clock = FakeClock()
        val first = clock.instant()
        val second = clock.instant()

        assertEquals(first, second)
    }

    @Test
    fun `advances by exactly the requested amount`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))

        clock.advanceBy(Duration.ofMinutes(40))

        assertEquals(Instant.parse("2026-01-01T00:40:00Z"), clock.instant())
    }

    @Test
    fun `advancing twice accumulates`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))

        clock.advanceBy(Duration.ofHours(1))
        clock.advanceBy(Duration.ofMinutes(30))

        assertEquals(Instant.parse("2026-01-01T01:30:00Z"), clock.instant())
    }

    @Test
    fun `refuses to run backwards`() {
        val clock = FakeClock()

        assertThrows(IllegalArgumentException::class.java) {
            clock.advanceBy(Duration.ofMinutes(-1))
        }
    }
}
