package com.okensun.todayfeed.components.feed.domain

import com.okensun.todayfeed.components.weather.api.WeatherRepository
import com.okensun.todayfeed.components.weather.api.models.Weather
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand written, not generated. The interface is small and was designed for this, so the fake is a
 * few lines and reads as a value rather than as a list of call expectations.
 */
class FakeWeatherRepository(
    weather: Weather? = null,
) : WeatherRepository {
    private val current = MutableStateFlow(weather)

    override fun observeCurrent(): Flow<Weather?> = current

    var refreshes = 0
        private set

    override suspend fun refresh() {
        refreshes++
    }

    fun emit(weather: Weather?) {
        current.value = weather
    }
}

fun weather(place: String = "Taipei") =
    Weather(
        placeName = place,
        temperatureCelsius = 30.0,
        condition = "Cloudy",
        highCelsius = 31.0,
        lowCelsius = 26.0
    )
