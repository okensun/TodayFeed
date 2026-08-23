package com.okensun.todayfeed.components.articles.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val articleId: String = savedStateHandle.get<String>(ARTICLE_ID_KEY).orEmpty()

        private val _state =
            MutableStateFlow<ContentState<String>>(
                if (articleId.isBlank()) {
                    ContentState.Error("That article could not be found.")
                } else {
                    ContentState.Content(articleId)
                }
            )
        val state: StateFlow<ContentState<String>> = _state.asStateFlow()

        private companion object {
            const val ARTICLE_ID_KEY = "articleId"
        }
    }
