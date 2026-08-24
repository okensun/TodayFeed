package com.okensun.todayfeed.components.articles.ui.saved

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
import com.okensun.todayfeed.components.articles.api.models.Article
import com.okensun.todayfeed.components.articles.ui.ArticleRowCard
import com.okensun.todayfeed.components.articles.ui.previewArticle
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.OfflineState
import com.okensun.todayfeed.core.designsystem.ThemePreviews
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme

/** Stateful form: finds the view model and nothing else. */
@Composable
fun SavedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavedScreen(
        state = state,
        onRetry = viewModel::onRetry,
        onArticleClick = onArticleClick,
        onToggleSave = viewModel::onToggleSave,
        modifier = modifier
    )
}

/**
 * Stateless form. Every case of [ContentState] is written out, with no `else`, for the same
 * reason as in the feed and the detail screen.
 *
 * `listState` is a parameter so that it lives above the `when` and the scroll position
 * survives a change of state.
 */
@Composable
internal fun SavedScreen(
    state: ContentState<List<Article>>,
    onRetry: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSave: (Article) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Content ->
            SavedList(state.value, listState, onArticleClick, onToggleSave, modifier)
        is ContentState.Empty -> EmptySaved(modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = onRetry,
                modifier = modifier
            )
        is ContentState.Offline -> {
            // Saved articles are stored when the user saves them, so this screen is not
            // expected to report being offline. It is handled the same way as the feed
            // rather than reused as the empty state, so the two stay distinguishable.
            val cached = state.cached
            if (cached == null) {
                OfflineState(onRetry = onRetry, modifier = modifier)
            } else {
                SavedList(cached, listState, onArticleClick, onToggleSave, modifier)
            }
        }
    }
}

@Composable
private fun EmptySaved(modifier: Modifier = Modifier) =
    EmptyState(
        title = "Nothing saved yet",
        body = "Open an article and save it. Saved articles stay readable without a network.",
        modifier = modifier
    )

@Composable
private fun SavedList(
    articles: List<Article>,
    listState: LazyListState,
    onArticleClick: (String) -> Unit,
    onToggleSave: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(articles, key = { it.id }) { article ->
            ArticleRowCard(
                article = article,
                onClick = { onArticleClick(article.id) },
                onToggleSave = { onToggleSave(article) }
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SavedContentPreview() = SavedPreview(ContentState.Content(listOf(previewArticle("1"), previewArticle("2"))))

@ThemePreviews
@Composable
private fun SavedLoadingPreview() = SavedPreview(ContentState.Loading)

@ThemePreviews
@Composable
private fun SavedEmptyPreview() = SavedPreview(ContentState.Empty)

@ThemePreviews
@Composable
private fun SavedErrorPreview() = SavedPreview(ContentState.Error("Could not read what you saved."))

@ThemePreviews
@Composable
private fun SavedOfflineCachedPreview() = SavedPreview(ContentState.Offline(listOf(previewArticle())))

@ThemePreviews
@Composable
private fun SavedOfflineEmptyPreview() = SavedPreview(ContentState.Offline(null))

@Composable
private fun SavedPreview(state: ContentState<List<Article>>) {
    TodayFeedTheme {
        SavedScreen(state = state, onRetry = {}, onArticleClick = {})
    }
}
