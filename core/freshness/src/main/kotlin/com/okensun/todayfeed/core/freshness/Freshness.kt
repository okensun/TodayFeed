package com.okensun.todayfeed.core.freshness

import java.time.Duration
import java.time.Instant

/**
 * Decides what to do about cached content.
 *
 * Every input is a parameter, including the time, so there is no clock to inject and no
 * connectivity to reach for. That is what makes the test a table.
 *
 * @param cachedAt when the content was stored, or null when nothing is stored.
 * @param serverMaxAge the maximum age the source stated for itself, if it stated one. It wins
 *   over [timeToLive], because a figure the source gives is a fact and ours is a judgement.
 * @param timeToLive our own figure, used when the source states none.
 */
fun decide(
    cachedAt: Instant?,
    serverMaxAge: Duration?,
    timeToLive: Duration,
    connection: Connection,
    now: Instant,
): Decision =
    when {
        // Offline is settled before age. With no network, how old the content is changes nothing
        // about what can be done, and calling it a refresh case produces a failure the reader
        // cannot act on.
        cachedAt == null && connection == Connection.Offline -> Decision.NothingToServe
        cachedAt == null -> Decision.Fetch
        connection == Connection.Offline -> Decision.ServeCacheStale
        isWithin(cachedAt, serverMaxAge ?: timeToLive, now) -> Decision.ServeCache
        else -> Decision.ServeCacheThenFetch
    }

/**
 * Content stamped in the future counts as within the allowance. Device and server clocks
 * disagree, and a reader whose clock is ahead should not have every page refetched.
 */
private fun isWithin(
    cachedAt: Instant,
    allowance: Duration,
    now: Instant,
): Boolean = Duration.between(cachedAt, now) <= allowance
