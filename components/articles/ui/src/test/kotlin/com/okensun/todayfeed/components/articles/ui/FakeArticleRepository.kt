package com.okensun.todayfeed.components.articles.ui

import android.database.sqlite.SQLiteException
import androidx.paging.PagingData
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * Storage a test can break on purpose. Both flags are read when the call is made, so one read can
 * be failed and the next let through, which is what a retry has to be driven with.
 */
internal class FakeArticleRepository : ArticleRepository {
    private val saved = MutableStateFlow<List<Article>>(emptyList())

    var readFails = false
    var writeFails = false
    val written = mutableListOf<String>()

    fun hold(vararg articles: Article) {
        saved.value = articles.toList()
    }

    override fun observeArticles(): Flow<PagingData<Article>> = emptyFlow()

    override fun observeSavedArticles(): Flow<List<Article>> =
        if (readFails) flow { throw SQLiteException("storage cannot be read") } else saved

    override suspend fun findArticle(id: String): Article? = saved.value.firstOrNull { it.id == id }

    override suspend fun save(id: String) = write(id)

    override suspend fun unsave(id: String) = write(id)

    private fun write(id: String) {
        if (writeFails) throw SQLiteException("storage is full")
        written += id
    }
}
