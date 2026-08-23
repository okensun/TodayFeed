package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.components.weather.api.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Builds the Reading list out of two independent sources.
 *
 * This is why `feed` has a domain module. Deciding the section order, and deciding what the
 * list looks like when one source has nothing, is logic that no single model can own. It
 * reaches both sources only through their `api` interfaces, so it can be tested with fakes
 * and no network.
 */
class ObserveFeed
    @Inject
    constructor(
        private val articles: ArticleRepository,
        private val weather: WeatherRepository,
    ) {
        operator fun invoke(): Flow<List<FeedItem>> =
            combine(weather.observeCurrent(), articles.observeFeed()) { currentWeather, articleList ->
                buildList {
                    // The hero card is pinned above the list, and is skipped rather than shown
                    // empty when the weather is not available. One source failing must not
                    // empty the feed.
                    if (currentWeather != null) add(FeedItem.WeatherHero(currentWeather))
                    articleList.forEach { add(FeedItem.ArticleRow(it)) }
                }
            }
    }
