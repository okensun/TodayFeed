package com.okensun.todayfeed.components.articles.ui

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel
    @Inject
    constructor(
        private val articles: ArticleRepository,
    ) : ViewModel() {
        private val retries = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<ContentState<List<Article>>> =
            retries
                .flatMapLatest {
                    // The failure is caught in here, so it ends this read and not the flow of
                    // retries. Caught outside, the first failure would carry `retries` away with
                    // it, and the button would raise a number nobody is left collecting.
                    articles
                        .observeSavedArticles()
                        .map { saved ->
                            if (saved.isEmpty()) ContentState.Empty else ContentState.Content(saved)
                        }.catch { emit(ContentState.Error("Saved articles could not be read.")) }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ContentState.Loading
                )

        /** Reading storage can fail, so the retry reads it again rather than doing nothing. */
        fun onRetry() {
            retries.value++
        }

        fun onToggleSave(article: Article) =
            viewModelScope.launch {
                try {
                    articles.toggleSaved(article.id)
                } catch (ignored: SQLiteException) {
                    // Storage is full or broken. There is nothing to put right, because what the
                    // star shows is read back from storage and so it never moved. Uncaught here
                    // it would take the app down instead.
                }
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
