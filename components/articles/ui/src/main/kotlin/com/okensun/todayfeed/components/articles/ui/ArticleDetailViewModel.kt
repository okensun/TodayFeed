package com.okensun.todayfeed.components.articles.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val articles: ArticleRepository,
    ) : ViewModel() {
        val articleId: String = savedStateHandle.get<String>(ARTICLE_ID_KEY).orEmpty()

        private val _state = MutableStateFlow<ContentState<Article>>(ContentState.Loading)
        val state: StateFlow<ContentState<Article>> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val article = articles.article(articleId)
                _state.value =
                    when (article) {
                        null -> ContentState.Error("That article could not be found.")
                        else -> ContentState.Content(article)
                    }
            }
        }

        private companion object {
            const val ARTICLE_ID_KEY = "articleId"
        }
    }
