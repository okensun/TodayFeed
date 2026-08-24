package com.okensun.todayfeed.components.feed.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
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
                // A refresh with content on screen is a marker, not a screen: the reader carries
                // on reading while it happens.
                if (paged.loadState.refresh is LoadState.Loading) {
                    item(key = REFRESHING) { Refreshing() }
                }
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
                when (val append = paged.loadState.append) {
                    is LoadState.Loading -> item(key = APPENDING) { Appending() }
                    // The failure belongs at the end of the list. Blanking what the reader holds
                    // because the next page did not arrive loses more than it explains.
                    is LoadState.Error ->
                        item(key = APPEND_FAILED) {
                            AppendFailed(append.error.message, paged::retry)
                        }
                    is LoadState.NotLoading -> Unit
                }
            }
    }
}

@Composable
private fun Refreshing() =
    LinearProgressIndicator(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = REFRESHING }
    )

@Composable
private fun Appending() =
    CircularProgressIndicator(
        modifier =
            Modifier
                .padding(16.dp)
                .semantics { contentDescription = APPENDING }
    )

@Composable
private fun AppendFailed(
    message: String?,
    onRetry: () -> Unit,
) = Column(modifier = Modifier.padding(16.dp)) {
    Text(
        text = message ?: COULD_NOT_LOAD_MORE,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TextButton(onClick = onRetry) { Text(TRY_AGAIN) }
}

// The indicators carry no text, so this is what a screen reader announces and what a view
// test finds them by.
private const val REFRESHING = "Refreshing"
private const val APPENDING = "Loading more"
private const val APPEND_FAILED = "append failed"
private const val COULD_NOT_LOAD_MORE = "More articles could not be loaded."

// The same word as the full screen error, so one action does not have two names.
private const val TRY_AGAIN = "Try again"

@Composable
private fun Section(section: FeedSection) =
    when (section) {
        is FeedSection.WeatherHero -> WeatherHeroCard(section.weather)
    }

private fun FeedSection.key(): String =
    when (this) {
        is FeedSection.WeatherHero -> "weather"
    }
