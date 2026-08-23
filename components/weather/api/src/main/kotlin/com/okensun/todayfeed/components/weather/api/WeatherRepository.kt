package com.okensun.todayfeed.components.weather.api

import kotlinx.coroutines.flow.Flow

/**
 * The contract for reading the current weather. Weather changes far faster than an article
 * does, so this is the source whose time-to-live is shortest. See the README.
 */
interface WeatherRepository {
    /** The weather for the feed's hero card, or null while nothing has been fetched yet. */
    fun observeCurrent(): Flow<Weather?>
}
