package com.okensun.todayfeed.components.movie.api.models

/**
 * One film. The year is text because the source sends it that way and nothing does arithmetic
 * with it. The score is a number because the row is ordered by it.
 */
data class Film(
    val id: String,
    val title: String,
    val year: String,
    val director: String,
    val bannerUrl: String?,
    val score: Int? = null,
)
