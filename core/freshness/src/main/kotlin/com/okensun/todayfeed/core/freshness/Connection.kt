package com.okensun.todayfeed.core.freshness

/**
 * What a byte costs right now: nothing, money, or it cannot be had.
 *
 * Named after metering rather than the transport because that is what the decisions keyed off it
 * care about. Android models metering as a capability separate from the transport and lets the
 * reader mark a Wi-Fi network as metered, so a phone tethering another phone is Wi-Fi and
 * metered, and an unlimited mobile plan is cellular and unmetered.
 */
enum class Connection {
    Unmetered,
    Metered,
    Offline,
}
