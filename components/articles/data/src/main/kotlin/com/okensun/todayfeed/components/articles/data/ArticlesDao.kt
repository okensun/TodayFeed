package com.okensun.todayfeed.components.articles.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

// A DAO is a list of queries, not a class doing too much, and the rule counting them is aimed
// at the second.
@Suppress("TooManyFunctions")
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

    /**
     * Writes what the source said and carries `savedAt` over. A plain upsert replaces every
     * column, so one refresh would quietly unsave everything the reader had kept.
     */
    @Transaction
    suspend fun upsertArticles(articles: List<ArticleEntity>) {
        val saved = savedTimes(articles.map { it.id }).associate { it.id to it.savedAt }
        upsertAll(articles.map { it.copy(savedAt = saved[it.id]) })
    }

    @Query("SELECT id, savedAt FROM articles WHERE id IN (:ids)")
    suspend fun savedTimes(ids: List<String>): List<SavedTime>

    @Upsert
    suspend fun upsertAll(articles: List<ArticleEntity>)

    /** Null unsaves. The time is also the order the Saved tab reads in. */
    @Query("UPDATE articles SET savedAt = :savedAt WHERE id = :id")
    suspend fun setSaved(
        id: String,
        savedAt: Instant?,
    )

    /**
     * Nothing here deletes, and nothing should: an article with a `savedAt` is the reader's, not
     * the cache's. Any tidying added later has to exclude these rows. See DECISIONS.md.
     */
    @Query("SELECT * FROM articles WHERE savedAt IS NOT NULL ORDER BY savedAt DESC")
    fun observeSaved(): Flow<List<ArticleEntity>>

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

/** Just enough of a row to carry the reader's decision through a refresh. */
internal data class SavedTime(
    val id: String,
    val savedAt: Instant?,
)
