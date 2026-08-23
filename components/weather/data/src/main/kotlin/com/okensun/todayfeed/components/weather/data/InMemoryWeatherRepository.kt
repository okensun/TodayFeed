package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.components.weather.api.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Fixed values for the skeleton. Slice 3 calls Open-Meteo behind the same interface. */
@Singleton
class InMemoryWeatherRepository
    @Inject
    constructor() : WeatherRepository {
        private val weather =
            MutableStateFlow<Weather?>(
                Weather(
                    placeName = "Taipei",
                    temperatureCelsius = 30.0,
                    condition = "Cloudy",
                    highCelsius = 31.0,
                    lowCelsius = 26.0
                )
            )

        override fun observeCurrent(): Flow<Weather?> = weather.asStateFlow()
    }
