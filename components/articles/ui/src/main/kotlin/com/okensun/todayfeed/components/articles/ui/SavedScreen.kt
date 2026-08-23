package com.okensun.todayfeed.components.articles.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import java.time.Instant

/** Stateful form: finds the view model and nothing else. */
@Composable
fun SavedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavedScreen(state = state, onArticleClick = onArticleClick, modifier = modifier)
}

@Composable
internal fun SavedScreen(
    state: ContentState<List<Article>>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = {},
                modifier = modifier
            )
        is ContentState.Content -> SavedList(state.value, listState, onArticleClick, modifier)
        is ContentState.Offline -> {
            val cached = state.cached
            if (cached == null) {
                // Saved articles are stored at the moment the user saves them, so being
                // offline with nothing here means nothing was ever saved.
                EmptySaved(modifier)
            } else {
                SavedList(cached, listState, onArticleClick, modifier)
            }
        }
        is ContentState.Empty -> EmptySaved(modifier)
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(articles) { article ->
            ArticleRowCard(article = article, onClick = { onArticleClick(article.id) })
        }
    }
}

private val previewSaved =
    listOf(
        Article(
            id = "1",
            title = "CNES seeks partners to mass produce compact optical telescopes",
            summary = "The French space agency is looking for an industrial partner.",
            source = "European Spaceflight",
            imageUrl = null,
            publishedAt = Instant.EPOCH
        )
    )

@Preview(name = "Saved content")
@Composable
private fun SavedContentPreview() = SavedPreview(ContentState.Content(previewSaved))

@Preview(name = "Saved empty")
@Composable
private fun SavedEmptyPreview() = SavedPreview(ContentState.Empty)

@Preview(name = "Saved offline with cache")
@Composable
private fun SavedOfflinePreview() = SavedPreview(ContentState.Offline(previewSaved))

@Composable
private fun SavedPreview(state: ContentState<List<Article>>) {
    TodayFeedTheme {
        SavedScreen(state = state, onArticleClick = {})
    }
}
