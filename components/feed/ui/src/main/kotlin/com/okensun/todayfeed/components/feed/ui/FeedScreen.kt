package com.okensun.todayfeed.components.feed.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.ui.ArticleRowCard
import com.okensun.todayfeed.components.feed.domain.FeedItem
import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.components.weather.ui.WeatherHeroCard
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.OfflineState
import com.okensun.todayfeed.core.designsystem.ThemePreviews
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import java.time.Instant

/**
 * The Reading screen, in its stateful form: it finds the view model and nothing else.
 *
 * Everything that draws lives in the stateless overload below, so each state can be seen in
 * a preview and checked without Hilt.
 */
@Composable
fun FeedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeedScreen(
        state = state,
        onRetry = viewModel::onRetry,
        onArticleClick = onArticleClick,
        modifier = modifier
    )
}

/**
 * The Reading screen, in its stateless form. This module is the only one allowed to depend
 * on other components' ui modules, because drawing their cards in one list is its whole job.
 *
 * `listState` is a parameter so that it is created above the `when`. Inside a branch it
 * would be dropped on any frame where that branch is not composed, which would reset the
 * scroll position when the state changes.
 */
@Composable
internal fun FeedScreen(
    state: ContentState<List<FeedItem>>,
    onRetry: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Empty -> EmptyState(title = "Nothing to read yet", modifier = modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = onRetry,
                modifier = modifier
            )
        is ContentState.Offline -> {
            // Bound to a local because Kotlin will not smart cast a public property that
            // belongs to another module. Offline with cached content still shows content.
            val cached = state.cached
            if (cached == null) {
                OfflineState(onRetry = onRetry, modifier = modifier)
            } else {
                FeedList(cached, listState, onArticleClick, modifier)
            }
        }
        is ContentState.Content -> FeedList(state.value, listState, onArticleClick, modifier)
    }
}

@Composable
private fun FeedList(
    items: List<FeedItem>,
    listState: LazyListState,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(items) { item ->
            when (item) {
                is FeedItem.WeatherHero -> WeatherHeroCard(item.weather)
                is FeedItem.ArticleRow ->
                    ArticleRowCard(
                        article = item.article,
                        onClick = { onArticleClick(item.article.id) }
                    )
            }
        }
    }
}

private val previewFeed =
    listOf(
        FeedItem.WeatherHero(
            Weather(
                placeName = "Taipei",
                temperatureCelsius = 30.0,
                condition = "Cloudy",
                highCelsius = 31.0,
                lowCelsius = 26.0
            )
        ),
        FeedItem.ArticleRow(
            Article(
                id = "1",
                title = "CNES seeks partners to mass produce compact optical telescopes",
                summary = "The French space agency is looking for an industrial partner.",
                source = "European Spaceflight",
                imageUrl = null,
                publishedAt = Instant.EPOCH
            )
        )
    )

@ThemePreviews
@Composable
private fun FeedContentPreview() = FeedPreview(ContentState.Content(previewFeed))

@ThemePreviews
@Composable
private fun FeedLoadingPreview() = FeedPreview(ContentState.Loading)

@ThemePreviews
@Composable
private fun FeedEmptyPreview() = FeedPreview(ContentState.Empty)

@ThemePreviews
@Composable
private fun FeedErrorPreview() = FeedPreview(ContentState.Error("Could not reach the server."))

@ThemePreviews
@Composable
private fun FeedOfflineEmptyPreview() = FeedPreview(ContentState.Offline(null))

@ThemePreviews
@Composable
private fun FeedOfflineCachedPreview() = FeedPreview(ContentState.Offline(previewFeed))

@Composable
private fun FeedPreview(state: ContentState<List<FeedItem>>) {
    TodayFeedTheme {
        FeedScreen(state = state, onRetry = {}, onArticleClick = {})
    }
}
