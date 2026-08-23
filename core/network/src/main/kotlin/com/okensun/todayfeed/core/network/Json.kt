package com.okensun.todayfeed.core.network

import kotlinx.serialization.json.Json

/**
 * Shared by every component's service. Unknown keys are ignored because a source is free to add
 * fields, and a feed that stops decoding because of a field nobody reads is a bad trade.
 */
val TodayFeedJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
