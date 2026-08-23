package com.okensun.todayfeed.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class CacheControlTest {
    /** The value this source actually sends, checked against the live API. */
    @Test
    fun `reads the value the article source sends`() {
        assertEquals(Duration.ofSeconds(600), maxAgeOf("max-age=600"))
    }

    @Test
    fun `finds max-age among other directives`() {
        assertEquals(Duration.ofSeconds(600), maxAgeOf("public, max-age=600, must-revalidate"))
        assertEquals(Duration.ofSeconds(30), maxAgeOf("private,max-age=30"))
    }

    @Test
    fun `is not confused by a similar directive`() {
        assertNull(maxAgeOf("s-maxage=600"))
        assertEquals(Duration.ofSeconds(600), maxAgeOf("max-age=600, s-maxage=99"))
    }

    @Test
    fun `a source that states nothing yields nothing`() {
        assertNull(maxAgeOf(null))
        assertNull(maxAgeOf(""))
        assertNull(maxAgeOf("no-cache"))
        assertNull(maxAgeOf("no-store, must-revalidate"))
    }

    @Test
    fun `a malformed value is ignored rather than guessed at`() {
        assertNull(maxAgeOf("max-age"))
        assertNull(maxAgeOf("max-age="))
        assertNull(maxAgeOf("max-age=soon"))
        assertNull(maxAgeOf("max-age=-1"))
    }

    @Test
    fun `zero means always stale, which is different from stating nothing`() {
        assertEquals(Duration.ZERO, maxAgeOf("max-age=0"))
    }
}
