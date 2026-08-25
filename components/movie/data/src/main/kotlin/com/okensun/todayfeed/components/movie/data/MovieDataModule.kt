package com.okensun.todayfeed.components.movie.data

import com.okensun.todayfeed.components.movie.api.FilmRepository
import com.okensun.todayfeed.components.movie.data.source.FilmService
import com.okensun.todayfeed.core.network.TodayFeedJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MovieDataModule {
    @Binds
    fun bindFilmRepository(impl: DefaultFilmRepository): FilmRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object FilmServiceModule {
    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun service(client: OkHttpClient): FilmService =
        Retrofit
            .Builder()
            .baseUrl("https://ghibliapi.vercel.app/")
            .client(client)
            .addConverterFactory(TodayFeedJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FilmService::class.java)
}
