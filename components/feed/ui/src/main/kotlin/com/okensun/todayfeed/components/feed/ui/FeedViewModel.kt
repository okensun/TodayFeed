package com.okensun.todayfeed.components.feed.ui

import androidx.lifecycle.ViewModel
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.feed.domain.FeedItem
import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Placeholder state only. Slice 2 replaces the fixed list with the real feed, built from
 * the cache and refreshed according to the freshness policy.
 */
@HiltViewModel
class FeedViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<ContentState<List<FeedItem>>>(
        ContentState.Content(placeholderFeed()),
    )
    val state: StateFlow<ContentState<List<FeedItem>>> = _state.asStateFlow()

    fun onRetry() = Unit

    private fun placeholderFeed(): List<FeedItem> = buildList {
        add(
            FeedItem.WeatherHero(
                Weather(
                    placeName = "Taipei",
                    temperatureCelsius = 30.0,
                    condition = "Cloudy",
                    highCelsius = 31.0,
                    lowCelsius = 26.0,
                ),
            ),
        )
        repeat(PLACEHOLDER_ROWS) { index ->
            add(
                FeedItem.ArticleRow(
                    Article(
                        id = "placeholder-$index",
                        title = "Placeholder article ${index + 1}",
                        summary = "Real articles arrive in slice 2.",
                        source = "Spaceflight News",
                        imageUrl = null,
                        publishedAt = Instant.EPOCH,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val PLACEHOLDER_ROWS = 6
    }
}
