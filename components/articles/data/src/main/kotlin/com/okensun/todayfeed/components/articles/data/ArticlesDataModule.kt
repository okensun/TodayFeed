package com.okensun.todayfeed.components.articles.data

import android.content.Context
import androidx.room.Room
import com.okensun.todayfeed.components.articles.api.ArticleRepository
import com.okensun.todayfeed.components.articles.data.database.ArticlesDatabase
import com.okensun.todayfeed.components.articles.data.database.MIGRATION_1_2
import com.okensun.todayfeed.components.articles.data.source.ArticlesService
import com.okensun.todayfeed.core.network.TodayFeedJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ArticlesDataModule {
    @Binds
    fun bindArticleRepository(impl: DefaultArticleRepository): ArticleRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object ArticlesDataProviders {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): ArticlesDatabase =
        Room
            .databaseBuilder(context, ArticlesDatabase::class.java, "articles.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    /**
     * The Retrofit instance is per source because the base address is per source. The client
     * underneath it is shared, so the connection pool and the timeouts are too.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun service(client: OkHttpClient): ArticlesService =
        Retrofit
            .Builder()
            .baseUrl("https://api.spaceflightnewsapi.net/")
            .client(client)
            .addConverterFactory(TodayFeedJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ArticlesService::class.java)
}
