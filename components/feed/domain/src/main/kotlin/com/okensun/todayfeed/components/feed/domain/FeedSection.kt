package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.movie.api.models.Film
import com.okensun.todayfeed.components.weather.api.models.Weather

/**
 * A block that sits above the article list rather than inside it.
 *
 * The articles are a paged stream and the sections are not, so they never meet. That makes "one
 * source failing must not empty the feed" something that cannot be violated rather than a rule
 * anyone has to maintain.
 */
sealed interface FeedSection {
    data class WeatherHero(
        val weather: Weather,
    ) : FeedSection

    data class Films(
        val films: List<Film>,
    ) : FeedSection
}
