package com.okensun.todayfeed.components.weather.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** A plain function over a number, so a plain test. */
class WeatherMappingTest {
    @Test
    fun `a code is grouped into words a reader can act on`() {
        assertEquals("Clear", conditionOf(0))
        assertEquals("Partly cloudy", conditionOf(2))
        assertEquals("Rain", conditionOf(65))
        assertEquals("Showers", conditionOf(80))
        assertEquals("Thunderstorm", conditionOf(99))
    }

    /** The source may add codes. Saying "Unknown" is better than showing a number. */
    @Test
    fun `a code nobody has seen still reads as words`() {
        assertEquals("Unknown", conditionOf(4))
        assertEquals("Unknown", conditionOf(123))
    }

    @Test
    fun `a day with no range falls back to the temperature it has`() {
        val weather = response(temperature = 25.5, high = emptyList(), low = emptyList()).toWeather("Taipei")

        assertEquals(25.5, weather.highCelsius, 0.001)
        assertEquals(25.5, weather.lowCelsius, 0.001)
    }

    @Test
    fun `the response becomes what the card needs`() {
        val weather = response(temperature = 25.5, code = 80).toWeather("Taipei")

        assertEquals("Taipei", weather.placeName)
        assertEquals(25.5, weather.temperatureCelsius, 0.001)
        assertEquals("Showers", weather.condition)
        assertEquals(28.1, weather.highCelsius, 0.001)
        assertEquals(25.0, weather.lowCelsius, 0.001)
    }

    private fun response(
        temperature: Double,
        code: Int = 0,
        high: List<Double> = listOf(28.1),
        low: List<Double> = listOf(25.0),
    ) = ForecastResponse(
        current = CurrentDto(temperatureCelsius = temperature, weatherCode = code),
        daily = DailyDto(high = high, low = low)
    )
}
