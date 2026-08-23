package com.okensun.todayfeed.components.articles.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState

@Composable
fun ArticleDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Error -> ErrorState(message = current.message, onRetry = onBack, modifier = modifier)
        else ->
            Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "Article ${viewModel.articleId}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text =
                        "The article body arrives in slice 2, together with the cache that " +
                            "makes it readable offline once it has been saved.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
    }
}
