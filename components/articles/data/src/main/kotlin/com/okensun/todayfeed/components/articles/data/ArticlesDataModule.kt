package com.okensun.todayfeed.components.articles.data

import com.okensun.todayfeed.components.articles.api.ArticleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ArticlesDataModule {
    @Binds
    fun bindArticleRepository(impl: InMemoryArticleRepository): ArticleRepository
}
