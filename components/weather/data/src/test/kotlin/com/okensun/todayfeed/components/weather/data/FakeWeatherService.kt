package com.okensun.todayfeed.components.weather.data

import retrofit2.Response

/** Records what was asked for, so a test can say "and then it asked for nothing". */
internal class FakeWeatherService(
    private val temperature: Double = 25.5,
) : WeatherService {
    var calls = 0
        private set

    override suspend fun forecast(
        latitude: Double,
        longitude: Double,
    ): Response<ForecastResponse> {
        calls++
        return Response.success(
            ForecastResponse(
                current = CurrentDto(temperatureCelsius = temperature, weatherCode = 0),
                daily = DailyDto(high = listOf(28.1), low = listOf(25.0))
            )
        )
    }
}
