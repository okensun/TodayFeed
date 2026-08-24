package com.okensun.todayfeed.components.articles.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface ArticlesDao {
    /**
     * The id breaks ties. Windows are read with `LIMIT` and `OFFSET`, so rows sharing a published
     * time could swap places between queries and be shown twice or skipped at a boundary.
     */
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC, id DESC")
    fun pagedArticles(): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun findArticle(id: String): ArticleEntity?

    /** How many of these are already stored. This is what tells a refresh it has reached back far enough. */
    @Query("SELECT COUNT(*) FROM articles WHERE id IN (:ids)")
    suspend fun countStored(ids: List<String>): Int

    @Upsert
    suspend fun upsertArticles(articles: List<ArticleEntity>)

    @Query("SELECT * FROM feed_metadata WHERE id = 0")
    suspend fun findMetadata(): FeedMetadataEntity?

    @Upsert
    suspend fun upsertMetadata(metadata: FeedMetadataEntity)

    /** Only the paging columns: an append must not write back a freshness stamp older than
     * its own request. */
    @Query("UPDATE feed_metadata SET nextOffset = :nextOffset, hasMore = :hasMore WHERE id = 0")
    suspend fun setPagingProgress(
        nextOffset: Int,
        hasMore: Boolean,
    )
}
