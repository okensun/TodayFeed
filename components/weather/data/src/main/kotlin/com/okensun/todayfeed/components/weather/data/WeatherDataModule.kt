package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.components.weather.api.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WeatherDataModule {
    @Binds
    fun bindWeatherRepository(impl: InMemoryWeatherRepository): WeatherRepository
}
