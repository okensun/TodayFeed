package com.okensun.todayfeed.components.articles.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

internal interface ArticlesService {
    /** Newest first, which is the source's own order. */
    @GET("v4/articles/")
    suspend fun articles(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): Response<ArticlesPage>
}

@Serializable
internal data class ArticlesPage(
    @SerialName("count") val count: Int,
    @SerialName("next") val next: String?,
    @SerialName("results") val results: List<ArticleDto>,
)

@Serializable
internal data class ArticleDto(
    // The source numbers its articles; we carry ids as text because another source may not.
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("summary") val summary: String,
    @SerialName("news_site") val newsSite: String,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("published_at") val publishedAt: String,
)
