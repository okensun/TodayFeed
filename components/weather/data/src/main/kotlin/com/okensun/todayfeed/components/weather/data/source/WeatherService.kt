package com.okensun.todayfeed.components.weather.data.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

internal interface WeatherService {
    /**
     * One call for now and today's range. What never varies is in the address rather than in the
     * signature, so only the place is a parameter.
     */
    @GET(
        "v1/forecast?current=temperature_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
    )
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): Response<ForecastResponse>
}

@Serializable
internal data class ForecastResponse(
    @SerialName("current") val current: CurrentDto,
    @SerialName("daily") val daily: DailyDto,
)

@Serializable
internal data class CurrentDto(
    @SerialName("temperature_2m") val temperatureCelsius: Double,
    @SerialName("weather_code") val weatherCode: Int,
)

@Serializable
internal data class DailyDto(
    @SerialName("temperature_2m_max") val high: List<Double>,
    @SerialName("temperature_2m_min") val low: List<Double>,
)
