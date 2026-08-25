package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.movie.api.FilmRepository
import com.okensun.todayfeed.components.weather.api.WeatherRepository
import javax.inject.Inject

/**
 * Asks the sources again. Each decides for itself whether that means a request, so the screen
 * does not need to know which sources exist or what their allowances are.
 */
class RefreshSections
    @Inject
    constructor(
        private val weather: WeatherRepository,
        private val films: FilmRepository,
    ) {
        suspend operator fun invoke() {
            weather.refresh()
            films.refresh()
        }
    }
