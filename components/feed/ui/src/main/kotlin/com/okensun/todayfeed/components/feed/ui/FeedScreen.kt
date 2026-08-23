package com.okensun.todayfeed.components.feed.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.components.articles.ui.ArticleRowCard
import com.okensun.todayfeed.components.feed.domain.FeedItem
import com.okensun.todayfeed.components.weather.ui.WeatherHeroCard
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.OfflineState

/**
 * The Reading screen. This module is the only one allowed to depend on other components'
 * ui modules, because drawing their cards in one list is its whole job.
 */
@Composable
fun FeedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Empty -> EmptyState(title = "Nothing to read yet", modifier = modifier)
        is ContentState.Error ->
            ErrorState(
                message = current.message,
                onRetry = viewModel::onRetry,
                modifier = modifier
            )
        is ContentState.Offline -> {
            // Bound to a local because Kotlin will not smart cast a public property that
            // belongs to another module. Offline with cached content still shows content.
            val cached = current.cached
            if (cached == null) {
                OfflineState(onRetry = viewModel::onRetry, modifier = modifier)
            } else {
                FeedList(items = cached, onArticleClick = onArticleClick, modifier = modifier)
            }
        }
        is ContentState.Content ->
            FeedList(
                items = current.value,
                onArticleClick = onArticleClick,
                modifier = modifier
            )
    }
}

@Composable
private fun FeedList(
    items: List<FeedItem>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
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
