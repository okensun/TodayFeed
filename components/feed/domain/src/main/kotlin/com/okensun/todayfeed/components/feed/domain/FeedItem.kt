package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.weather.api.Weather

/**
 * One row in the Reading list. The feed is heterogeneous, so this is a sealed type and the
 * screen renders it with a `when`. Each case carries an api model straight from the
 * component that owns it, with no per-card mapping layer in between.
 */
sealed interface FeedItem {
    data class WeatherHero(val weather: Weather) : FeedItem
    data class ArticleRow(val article: Article) : FeedItem
}
