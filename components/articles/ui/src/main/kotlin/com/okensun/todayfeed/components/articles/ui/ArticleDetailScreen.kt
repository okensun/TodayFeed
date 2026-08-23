package com.okensun.todayfeed.components.articles.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.ErrorState
import com.okensun.todayfeed.core.designsystem.LoadingState
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import java.time.Instant

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

@Composable
internal fun ArticleDetailScreen(
    state: ContentState<Article>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        is ContentState.Content -> ArticleBody(state.value, modifier)
        is ContentState.Error ->
            ErrorState(
                message = state.message,
                onRetry = onBack,
                modifier = modifier
            )
        // There is no useful empty or offline detail screen: either the article was stored
        // when the user saved it, or it was never there.
        else ->
            ErrorState(
                message = "That article could not be found.",
                onRetry = onBack,
                modifier = modifier
            )
    }
}

@Composable
private fun ArticleBody(
    article: Article,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

@Preview(name = "Detail content")
@Composable
private fun DetailContentPreview() =
    TodayFeedTheme {
        ArticleDetailScreen(
            state =
                ContentState.Content(
                    Article(
                        id = "1",
                        title = "CNES seeks partners to mass produce compact optical telescopes",
                        summary =
                            "The French space agency is looking for an industrial partner to " +
                                "develop and qualify a compact optical telescope for future satellite " +
                                "constellations.",
                        source = "European Spaceflight",
                        imageUrl = null,
                        publishedAt = Instant.EPOCH
                    )
                ),
            onBack = {}
        )
    }

@Preview(name = "Detail unknown id")
@Composable
private fun DetailErrorPreview() =
    TodayFeedTheme {
        ArticleDetailScreen(state = ContentState.Error("That article could not be found."), onBack = {})
    }
