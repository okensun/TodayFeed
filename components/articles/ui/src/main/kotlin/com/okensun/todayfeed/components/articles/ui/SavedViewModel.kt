package com.okensun.todayfeed.components.articles.ui

import androidx.lifecycle.ViewModel
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject

/**
 * Placeholder state. Slice 3 replaces the fixed row with what the user actually saved,
 * read from the cache so it stays readable with no network.
 */
@HiltViewModel
class SavedViewModel
    @Inject
    constructor() : ViewModel() {
        private val _state =
            MutableStateFlow<ContentState<List<Article>>>(
                ContentState.Content(
                    listOf(
                        Article(
                            id = "placeholder-saved",
                            title = "Placeholder saved article",
                            summary = "Saving arrives in slice 3.",
                            source = "Spaceflight News",
                            imageUrl = null,
                            publishedAt = Instant.EPOCH
                        )
                    )
                )
            )
        val state: StateFlow<ContentState<List<Article>>> = _state.asStateFlow()
    }
