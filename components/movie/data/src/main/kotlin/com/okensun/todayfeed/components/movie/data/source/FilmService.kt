package com.okensun.todayfeed.components.movie.data.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET

internal interface FilmService {
    /** The whole catalogue in one answer. Twenty-two films, about 32 KB. */
    @GET("films")
    suspend fun films(): Response<List<FilmDto>>
}

@Serializable
internal data class FilmDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("director") val director: String,
    @SerialName("movie_banner") val movieBanner: String? = null,
    @SerialName("rt_score") val rtScore: String? = null,
)
