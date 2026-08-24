package com.okensun.todayfeed.components.feed.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.ui.ArticleRowCard
import com.okensun.todayfeed.components.feed.domain.FeedSection
import com.okensun.todayfeed.components.weather.ui.WeatherHeroCard
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import kotlinx.coroutines.flow.Flow

/** Stateful form: finds the view model and nothing else. */
@Composable
fun FeedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    FeedScreen(
        articles = viewModel.articles,
        sections = sections,
        onArticleClick = onArticleClick,
        modifier = modifier
    )
}

/**
 * Stateless form. This module is the only one allowed to depend on other components' ui modules,
 * because drawing their cards in one list is its whole job.
 */
@Composable
internal fun FeedScreen(
    articles: Flow<PagingData<Article>>,
    sections: List<FeedSection>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val paged = articles.collectAsLazyPagingItems()

    when (val state = feedContentState(paged.loadState.refresh, paged.itemCount, sections.isNotEmpty())) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Empty -> EmptyState(title = "Nothing to read yet", modifier = modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = paged::retry,
                modifier = modifier
            )
        // Offline arrives in pass 3, once the connection is read for real.
        is ContentState.Offline, is ContentState.Content ->
            LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
                sections.forEach { section ->
                    item(key = section.key()) { Section(section) }
                }
                // Keyed on the article's own id. Without a key LazyColumn keys by index, so a
                // refresh that upserts newer articles would move every row and throw the reader's
                // position off by however many arrived — the opposite of keeping their place.
                items(
                    count = paged.itemCount,
                    key = { index -> paged.peek(index)?.id ?: index }
                ) { index ->
                    paged[index]?.let { article ->
                        ArticleRowCard(
                            article = article,
                            onClick = { onArticleClick(article.id) }
                        )
                    }
                }
            }
    }
}

@Composable
private fun Section(section: FeedSection) =
    when (section) {
        is FeedSection.WeatherHero -> WeatherHeroCard(section.weather)
    }

private fun FeedSection.key(): String =
    when (this) {
        is FeedSection.WeatherHero -> "weather"
    }
