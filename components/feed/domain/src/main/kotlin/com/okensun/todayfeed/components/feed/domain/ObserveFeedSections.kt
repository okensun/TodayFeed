package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.weather.api.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The ordered list of sections above the feed, and what to leave out when a source has nothing.
 *
 * Thin while weather is the only section, and stated in terms of the list rather than the count,
 * so a second source is one case and one constructor parameter. It reaches its sources only
 * through their `api` interfaces, so it needs no network and no paging.
 */
class ObserveFeedSections
    @Inject
    constructor(
        private val weather: WeatherRepository,
    ) {
        operator fun invoke(): Flow<List<FeedSection>> =
            weather.observeCurrent().map { current ->
                buildList {
                    // Skipped rather than shown empty. A section with nothing to say should not take up
                    // the top of the screen.
                    if (current != null) add(FeedSection.WeatherHero(current))
                }
            }
    }
