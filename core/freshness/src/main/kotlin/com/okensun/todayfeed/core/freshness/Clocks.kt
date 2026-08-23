package com.okensun.todayfeed.core.freshness

import java.time.Clock

/**
 * The freshness policy is entirely about how old something is, so time is injected rather
 * than read from the system. `java.time.Clock` is used directly instead of a home made
 * interface: it already has the shape we need, and core library desugaring makes it
 * available on API 24. See DECISIONS.md.
 */
fun systemClock(): Clock = Clock.systemUTC()
