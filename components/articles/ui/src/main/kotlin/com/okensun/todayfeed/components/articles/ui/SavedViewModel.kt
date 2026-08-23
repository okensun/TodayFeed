package com.okensun.todayfeed.components.articles.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SavedViewModel
    @Inject
    constructor(
        articles: ArticleRepository,
    ) : ViewModel() {
        val state: StateFlow<ContentState<List<Article>>> =
            articles
                .observeSaved()
                .map { saved ->
                    if (saved.isEmpty()) ContentState.Empty else ContentState.Content(saved)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ContentState.Loading
                )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
