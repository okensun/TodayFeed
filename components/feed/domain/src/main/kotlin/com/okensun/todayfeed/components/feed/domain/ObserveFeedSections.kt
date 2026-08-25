package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.movie.api.FilmRepository
import com.okensun.todayfeed.components.weather.api.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        private val films: FilmRepository,
    ) {
        /** The order the sections are built in is the order they appear: weather, films, then
         * the articles below them. */
        operator fun invoke(): Flow<List<FeedSection>> =
            combine(weather.observeCurrent(), films.observeFilms()) { current, catalogue ->
                buildList {
                    // Skipped rather than shown empty. A section with nothing to say should not
                    // take up the top of the screen, and one source failing must not cost another.
                    if (current != null) add(FeedSection.WeatherHero(current))
                    if (catalogue.isNotEmpty()) add(FeedSection.Films(catalogue))
                }
            }
    }
