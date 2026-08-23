package com.okensun.todayfeed.components.articles.api

import java.time.Instant

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val imageUrl: String?,
    val publishedAt: Instant,
)
