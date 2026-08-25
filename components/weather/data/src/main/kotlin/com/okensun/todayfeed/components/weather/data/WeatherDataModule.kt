package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.components.weather.api.WeatherRepository
import com.okensun.todayfeed.components.weather.data.source.WeatherService
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
internal interface WeatherDataModule {
    @Binds
    fun bindWeatherRepository(impl: DefaultWeatherRepository): WeatherRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object WeatherServiceModule {
    /**
     * Its own Retrofit because the base address is its own. The client underneath is shared, so
     * the connection pool and the timeouts are too.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun service(client: OkHttpClient): WeatherService =
        Retrofit
            .Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(TodayFeedJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WeatherService::class.java)
}
