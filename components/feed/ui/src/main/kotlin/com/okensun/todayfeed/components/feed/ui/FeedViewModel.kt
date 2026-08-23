package com.okensun.todayfeed.components.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okensun.todayfeed.components.feed.domain.FeedItem
import com.okensun.todayfeed.components.feed.domain.ObserveFeed
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        observeFeed: ObserveFeed,
    ) : ViewModel() {
        val state: StateFlow<ContentState<List<FeedItem>>> =
            observeFeed()
                .map { items ->
                    if (items.isEmpty()) ContentState.Empty else ContentState.Content(items)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = ContentState.Loading
                )

        fun onRetry() = Unit

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
