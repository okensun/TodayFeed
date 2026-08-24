package com.okensun.todayfeed.components.feed.ui

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.components.articles.api.models.Article
import com.okensun.todayfeed.components.feed.domain.FeedSection
import com.okensun.todayfeed.components.feed.domain.ObserveFeedSections
import com.okensun.todayfeed.components.feed.domain.ObserveNetworkReturned
import com.okensun.todayfeed.components.feed.domain.ObserveOffline
import com.okensun.todayfeed.components.feed.domain.RefreshSections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        private val repository: ArticleRepository,
        observeSections: ObserveFeedSections,
        observeOffline: ObserveOffline,
        observeNetworkReturned: ObserveNetworkReturned,
        private val refreshSections: RefreshSections,
    ) : ViewModel() {
        init {
            onRefreshSections()
        }

        /** `cachedIn` so the loaded pages survive the activity being recreated. */
        val articles: Flow<PagingData<Article>> = repository.observeArticles().cachedIn(viewModelScope)

        val sections: StateFlow<List<FeedSection>> =
            observeSections()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = emptyList()
                )

        /** Not a failure, so it is its own flag rather than a kind of error. */
        val offline: StateFlow<Boolean> =
            observeOffline()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = false
                )

        /** The screen asks again when this fires, because nothing else will. */
        val networkReturned: Flow<Unit> = observeNetworkReturned()

        /** The paged articles refresh themselves; the sections have to be asked. */
        fun onRefreshSections() =
            viewModelScope.launch {
                refreshSections()
            }

        fun onToggleSave(article: Article) =
            viewModelScope.launch {
                try {
                    repository.toggleSaved(article.id)
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
