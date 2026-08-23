package com.okensun.todayfeed.core.freshness

/**
 * What to do about a cached thing. Every case is final: the caller acts on it without having to
 * work anything else out, which is why there are five and not four.
 */
sealed interface Decision {
    /** Young enough that asking would change nothing. */
    data object ServeCache : Decision

    /** Past its allowance, and something is stored. Show that, refresh behind it. */
    data object ServeCacheThenFetch : Decision

    /** Nothing is stored. The reader has to wait. */
    data object Fetch : Decision

    /** No network, but something is stored. Show it and say it may be old. */
    data object ServeCacheStale : Decision

    /** No network and nothing stored. */
    data object NothingToServe : Decision
}
