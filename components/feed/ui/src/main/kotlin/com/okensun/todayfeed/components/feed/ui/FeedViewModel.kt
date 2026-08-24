package com.okensun.todayfeed.components.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.components.feed.domain.FeedSection
import com.okensun.todayfeed.components.feed.domain.ObserveFeedSections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        articles: ArticleRepository,
        observeSections: ObserveFeedSections,
    ) : ViewModel() {
        /** `cachedIn` so the loaded pages survive the activity being recreated. */
        val articles: Flow<PagingData<Article>> = articles.pagedFeed().cachedIn(viewModelScope)

        val sections: StateFlow<List<FeedSection>> =
            observeSections()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = emptyList()
                )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
