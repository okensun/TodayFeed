package com.okensun.todayfeed.components.weather.data

import com.okensun.todayfeed.components.weather.api.models.Weather
import com.okensun.todayfeed.components.weather.data.source.ForecastResponse

/**
 * The source answers with a WMO code. Grouped rather than listed one by one: a reader wants to
 * know whether to take a coat, not which of four kinds of drizzle it is.
 */
@Suppress("MagicNumber")
internal fun conditionOf(code: Int): String =
    when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Unknown"
    }

/** A day with no range still has a temperature, so the card shows what it has. */
internal fun ForecastResponse.toWeather(placeName: String) =
    Weather(
        placeName = placeName,
        temperatureCelsius = current.temperatureCelsius,
        condition = conditionOf(current.weatherCode),
        highCelsius = daily.high.firstOrNull() ?: current.temperatureCelsius,
        lowCelsius = daily.low.firstOrNull() ?: current.temperatureCelsius
    )
