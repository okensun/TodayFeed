package com.okensun.todayfeed.components.articles.ui.detail

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.components.articles.api.models.Article
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
            load()
        }

        /**
         * The id comes from the arguments rather than from what is on screen, so nothing here goes
         * by a snapshot. An id that is not stored changes nothing, which is what the early return
         * used to be for.
         */
        fun onToggleSave() =
            viewModelScope.launch {
                try {
                    articles.toggleSaved(articleId)
                } catch (ignored: SQLiteException) {
                    // Storage is full or broken. Reading again below shows what is really stored,
                    // so the star settles on the truth. Uncaught it would take the app down.
                }
                load()
            }

        /** Read from storage, not from the paged feed. A saved article can fall out of what the
         * source returns and must still open. */
        private fun load() =
            viewModelScope.launch {
                val article = articles.findArticle(articleId)
                _state.value =
                    when (article) {
                        null -> ContentState.Error("That article could not be found.")
                        else -> ContentState.Content(article)
                    }
            }

        private companion object {
            const val ARTICLE_ID_KEY = "articleId"
        }
    }
