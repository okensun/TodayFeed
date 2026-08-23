package com.okensun.todayfeed.components.weather.api

data class Weather(
    val placeName: String,
    val temperatureCelsius: Double,
    val condition: String,
    val highCelsius: Double,
    val lowCelsius: Double,
)
