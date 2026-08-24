package com.okensun.todayfeed.components.articles.api

import java.time.Instant

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val imageUrl: String?,
    val publishedAt: Instant,
    /** Whether the reader kept this one. Carried on the model so every list that shows an
     * article shows its state without being told separately. */
    val saved: Boolean = false,
)
