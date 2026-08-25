package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.movie.api.models.Film
import com.okensun.todayfeed.components.weather.api.models.Weather

/**
 * A block that sits above the article list rather than inside it.
 *
 * The articles are a paged stream and the sections are not, so they never meet. That makes "one
 * source failing must not empty the feed" something that cannot be violated rather than a rule
 * anyone has to maintain.
 *
 * A section is here before its source has answered, holding nothing. Its place on screen is
 * therefore taken from the first frame, so a source answering late changes what is inside a block
 * and never how many blocks there are.
 */
sealed interface FeedSection {
    /** Whether the source has answered with something. Nothing yet is a shape, not content. */
    val hasContent: Boolean

    data class WeatherHero(
        val weather: Weather?,
    ) : FeedSection {
        override val hasContent: Boolean get() = weather != null
    }

    data class Films(
        val films: List<Film>,
    ) : FeedSection {
        override val hasContent: Boolean get() = films.isNotEmpty()
    }
}
