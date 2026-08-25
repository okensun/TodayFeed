package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.movie.api.FilmRepository
import com.okensun.todayfeed.components.weather.api.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The ordered list of sections above the feed. Every section is always in it, answered or not.
 *
 * Leaving one out until its source answered made the list grow under the reader, and a block
 * appearing above what someone is reading either moves them or goes unseen. Only the contents of a
 * block change now, so what is above the reader never appears or disappears. What an unanswered
 * block looks like is the screen's business, not this one's.
 *
 * It reaches its sources only through their `api` interfaces, so it needs no network and no paging.
 */
class ObserveFeedSections
    @Inject
    constructor(
        private val weather: WeatherRepository,
        private val films: FilmRepository,
    ) {
        /** The order they are built in is the order they appear: weather, films, then the
         * articles below them. */
        operator fun invoke(): Flow<List<FeedSection>> =
            combine(weather.observeCurrent(), films.observeFilms()) { current, catalogue ->
                listOf(FeedSection.WeatherHero(current), FeedSection.Films(catalogue))
            }
    }
