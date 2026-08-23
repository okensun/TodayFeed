package com.okensun.todayfeed.components.articles.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.OfflineState
import com.okensun.todayfeed.core.designsystem.ThemePreviews
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme

/** Stateful form: finds the view model and nothing else. */
@Composable
fun ArticleDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ArticleDetailScreen(state = state, onBack = onBack, modifier = modifier)
}

/**
 * Stateless form. Every case of [ContentState] is written out, with no `else`: an `else` here
 * would let a new case reach production as a wrong screen with nothing to warn about.
 *
 * `scrollState` is a parameter so that it lives above the `when`, which is what keeps the
 * reading position when the state changes.
 */
@Composable
internal fun ArticleDetailScreen(
    state: ContentState<Article>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Content -> ArticleBody(state.value, scrollState, modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = onBack,
                modifier = modifier
            )
        is ContentState.Offline -> {
            // An article can be in the cache without ever having been saved, so being
            // offline is not the same as the article being missing. Show what we have.
            val cached = state.cached
            if (cached == null) {
                OfflineState(onRetry = onBack, modifier = modifier)
            } else {
                ArticleBody(cached, scrollState, modifier)
            }
        }
        // A single article is either there or it is not, so there is no useful empty state.
        is ContentState.Empty ->
            ErrorState(
                message = ARTICLE_NOT_FOUND,
                onRetry = onBack,
                modifier = modifier
            )
    }
}

private const val ARTICLE_NOT_FOUND = "That article could not be found."

@Composable
private fun ArticleBody(
    article: Article,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
    ) {
        Text(text = article.title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = article.source,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = article.summary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@ThemePreviews
@Composable
private fun DetailContentPreview() = DetailPreview(ContentState.Content(previewArticle()))

@ThemePreviews
@Composable
private fun DetailLoadingPreview() = DetailPreview(ContentState.Loading)

@ThemePreviews
@Composable
private fun DetailErrorPreview() = DetailPreview(ContentState.Error(ARTICLE_NOT_FOUND))

@ThemePreviews
@Composable
private fun DetailEmptyPreview() = DetailPreview(ContentState.Empty)

@ThemePreviews
@Composable
private fun DetailOfflineCachedPreview() = DetailPreview(ContentState.Offline(previewArticle()))

@ThemePreviews
@Composable
private fun DetailOfflineEmptyPreview() = DetailPreview(ContentState.Offline(null))

@Composable
private fun DetailPreview(state: ContentState<Article>) {
    TodayFeedTheme {
        ArticleDetailScreen(state = state, onBack = {})
    }
}
