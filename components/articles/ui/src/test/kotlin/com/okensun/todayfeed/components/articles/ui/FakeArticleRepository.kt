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
 *
 * [kept] flips the way storage does, so a test can tap the same article twice and see where it
 * ends up rather than only what was called.
 */
internal class FakeArticleRepository : ArticleRepository {
    private val articles = MutableStateFlow<List<Article>>(emptyList())

    var readFails = false
    var writeFails = false
    val kept = mutableSetOf<String>()

    fun hold(vararg held: Article) {
        articles.value = held.toList()
    }

    override fun observeArticles(): Flow<PagingData<Article>> = emptyFlow()

    override fun observeSavedArticles(): Flow<List<Article>> =
        if (readFails) flow { throw SQLiteException("storage cannot be read") } else articles

    override suspend fun findArticle(id: String): Article? = articles.value.firstOrNull { it.id == id }

    override suspend fun toggleSaved(id: String) {
        if (writeFails) throw SQLiteException("storage is full")
        if (!kept.add(id)) kept.remove(id)
    }
}
