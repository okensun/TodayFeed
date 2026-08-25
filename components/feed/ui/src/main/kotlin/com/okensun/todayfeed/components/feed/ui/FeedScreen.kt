package com.okensun.todayfeed.components.feed.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.okensun.todayfeed.components.articles.api.models.Article
import com.okensun.todayfeed.components.articles.ui.ArticleRowCard
import com.okensun.todayfeed.components.feed.domain.FeedSection
import com.okensun.todayfeed.components.movie.ui.FilmCarouselCard
import com.okensun.todayfeed.components.movie.ui.FilmCarouselPlaceholder
import com.okensun.todayfeed.components.weather.ui.WeatherHeroCard
import com.okensun.todayfeed.components.weather.ui.WeatherHeroPlaceholder
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.OfflineState
import com.okensun.todayfeed.core.designsystem.SectionTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Stateful form: finds the view model and nothing else. */
@Composable
fun FeedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    FeedScreen(
        articles = viewModel.articles,
        sections = sections,
        offline = offline,
        refreshWhen = viewModel.networkReturned,
        onArticleClick = onArticleClick,
        onRefreshSections = viewModel::onRefreshSections,
        onToggleSave = viewModel::onToggleSave,
        modifier = modifier
    )
}

/**
 * Stateless form. This module is the only one allowed to depend on other components' ui modules,
 * because drawing their cards in one list is its whole job.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreen(
    articles: Flow<PagingData<Article>>,
    sections: List<FeedSection>,
    offline: Boolean,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRefreshSections: () -> Unit = {},
    onToggleSave: (Article) -> Unit = {},
    refreshWhen: Flow<Unit> = emptyFlow(),
    listState: LazyListState = rememberLazyListState(),
) {
    val paged = articles.collectAsLazyPagingItems()

    RefreshOn(refreshWhen) {
        paged.refresh()
        onRefreshSections()
    }
    val told = sections.any { it.hasContent }
    val state = feedContentState(paged.loadState.refresh, paged.itemCount, told, offline)
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Empty -> EmptyState(title = "Nothing to read yet", modifier = modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = paged::retry,
                modifier = modifier
            )
        // Nothing stored and no connection is the one dead end, and even it offers a retry.
        is ContentState.Offline if state.cached == null ->
            OfflineState(onRetry = paged::refresh, modifier = modifier)
        is ContentState.Offline, is ContentState.Content ->
            RefreshableFeed(
                refreshing = paged.loadState.refresh is LoadState.Loading,
                onRefresh = {
                    paged.refresh()
                    onRefreshSections()
                },
                modifier = modifier
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Outside the list on purpose. As a list item it would sit above the anchor
                    // that keeps the reader's place, so losing the network would show nothing
                    // until they scrolled up to find it.
                    if (offline) {
                        OutOfDate()
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        sections.forEach { section ->
                            item(key = section.key()) { Section(section, offline) }
                        }
                        if (paged.itemCount > 0) {
                            item(key = ARTICLES) { SectionTitle(ARTICLES) }
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
                                    onClick = { onArticleClick(article.id) },
                                    onToggleSave = { onToggleSave(article) }
                                )
                            }
                        }
                        appendState(paged.loadState.append, paged::retry)
                    }
                }
            }
    }
}

/** `refresh()` skips `initialize()`, which is where the freshness policy lives, so a pull asks
 * the source whatever the allowance says. That is what a pull is for. */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Watched with the lifecycle rather than with the composition. A composition outlives the app
 * going to the background, so on its own it would keep the platform callback registered and
 * fetch when the connection came back with nobody looking.
 */
@Composable
private fun RefreshOn(
    signal: Flow<Unit>,
    refresh: () -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(signal, refresh, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            signal.collect { refresh() }
        }
    }
}

@Composable
private fun RefreshableFeed(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .semantics { contentDescription = REFRESHING }
            )
        }
    ) { content() }
}

@Composable
private fun Appending() =
    CircularProgressIndicator(
        modifier =
            Modifier
                .padding(16.dp)
                .semantics { contentDescription = APPENDING }
    )

@Composable
private fun OutOfDate() =
    Text(
        text = OUT_OF_DATE,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
private const val OUT_OF_DATE = "You are offline. These articles may be out of date."
private const val COULD_NOT_LOAD_MORE = "More articles could not be loaded."

// Every band of the feed is named the same way, so one kind of content cannot be taken for
// another.
private const val ARTICLES = "Articles"

// The same word as the full screen error, so one action does not have two names.
private const val TRY_AGAIN = "Try again"

/**
 * The failure belongs at the end of the list. Blanking what the reader holds because the next
 * page did not arrive loses more than it explains.
 */
private fun LazyListScope.appendState(
    append: LoadState,
    onRetry: () -> Unit,
) = when (append) {
    is LoadState.Loading -> item(key = APPENDING) { Appending() }
    is LoadState.Error ->
        item(key = APPEND_FAILED) { AppendFailed(append.error.message, onRetry) }
    is LoadState.NotLoading -> Unit
}

/**
 * A source that has not answered keeps its block rather than losing it. Offline is what turns
 * waiting into an answer: with no connection there is nothing still on its way.
 */
@Composable
private fun Section(
    section: FeedSection,
    offline: Boolean,
) = when (section) {
    is FeedSection.WeatherHero ->
        section.weather?.let { WeatherHeroCard(it) } ?: WeatherHeroPlaceholder(missing = offline)
    is FeedSection.Films ->
        if (section.films.isEmpty()) {
            FilmCarouselPlaceholder(missing = offline)
        } else {
            FilmCarouselCard(section.films)
        }
}

private fun FeedSection.key(): String =
    when (this) {
        is FeedSection.WeatherHero -> "weather"
        is FeedSection.Films -> "films"
    }
