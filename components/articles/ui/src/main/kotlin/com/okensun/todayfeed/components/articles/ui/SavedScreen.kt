package com.okensun.todayfeed.components.articles.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.LoadingState

@Composable
fun SavedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Hoisted for the same reason as in FeedScreen: a state created inside a branch is
    // dropped on any frame where that branch is not composed.
    val listState = rememberLazyListState()

    when (val current = state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Content ->
            LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
                items(current.value) { article ->
                    ArticleRowCard(article = article, onClick = { onArticleClick(article.id) })
                }
            }
        else ->
            EmptyState(
                title = "Nothing saved yet",
                body = "Open an article and save it. Saved articles stay readable without a network.",
                modifier = modifier
            )
    }
}
