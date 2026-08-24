package com.okensun.todayfeed.core.network

import java.time.Duration

/**
 * Reads `max-age` out of a `Cache-Control` header.
 *
 * A maximum age the source states about itself is a fact; the figure the app carries is a
 * judgement. Where both exist the fact wins, so this is what the freshness decision prefers.
 */
fun maxAgeOf(cacheControl: String?): Duration? =
    cacheControl
        ?.split(',')
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith(MAX_AGE, ignoreCase = true) }
        ?.substringAfter('=', missingDelimiterValue = "")
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it >= 0 }
        ?.let(Duration::ofSeconds)

private const val MAX_AGE = "max-age"
