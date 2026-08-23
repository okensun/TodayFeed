package com.okensun.todayfeed.components.articles.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import com.okensun.todayfeed.core.designsystem.EmptyState
import com.okensun.todayfeed.core.designsystem.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SavedViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<ContentState<List<Article>>>(ContentState.Empty)
    val state: StateFlow<ContentState<List<Article>>> = _state.asStateFlow()
}

@Composable
fun SavedScreen(
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        is ContentState.Loading -> LoadingState(modifier)
        else -> EmptyState(
            title = "Nothing saved yet",
            body = "Open an article and save it. Saved articles stay readable without a network.",
            modifier = modifier,
        )
    }
}
