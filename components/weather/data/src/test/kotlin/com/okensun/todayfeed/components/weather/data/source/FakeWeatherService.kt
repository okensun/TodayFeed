package com.okensun.todayfeed.components.weather.data.source

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/** Records what was asked for, so a test can say "and then it asked for nothing". */
internal class FakeWeatherService(
    private val temperature: Double = 25.5,
) : WeatherService {
    var calls = 0
        private set

    /** Set to make the next call fail the way a real one can. */
    var failure: Failure? = null

    enum class Failure {
        NoNetwork,
        ServerError,
    }

    override suspend fun forecast(
        latitude: Double,
        longitude: Double,
    ): Response<ForecastResponse> {
        calls++
        return when (failure) {
            Failure.NoNetwork -> throw IOException("no network")
            Failure.ServerError ->
                Response.error(500, "".toResponseBody("application/json".toMediaType()))
            null ->
                Response.success(
                    ForecastResponse(
                        current = CurrentDto(temperatureCelsius = temperature, weatherCode = 0),
                        daily = DailyDto(high = listOf(28.1), low = listOf(25.0))
                    )
                )
        }
    }
}
