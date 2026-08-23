package com.okensun.todayfeed.components.weather.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.okensun.todayfeed.components.weather.api.Weather
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherHeroCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `shows the place, the temperature and the high and low`() {
        compose.setContent {
            WeatherHeroCard(
                Weather(
                    placeName = "Taipei",
                    temperatureCelsius = 30.4,
                    condition = "Cloudy",
                    highCelsius = 31.0,
                    lowCelsius = 26.0
                )
            )
        }

        compose.onNodeWithText("Taipei").assertIsDisplayed()
        compose.onNodeWithText("30°").assertIsDisplayed()
        compose.onNodeWithText("Cloudy").assertIsDisplayed()
        compose.onNodeWithText("  H 31°  L 26°").assertIsDisplayed()
    }

    @Test
    fun `rounds the temperature down rather than showing decimals`() {
        compose.setContent {
            WeatherHeroCard(
                Weather(
                    placeName = "Taipei",
                    temperatureCelsius = 30.9,
                    condition = "Clear",
                    highCelsius = 31.0,
                    lowCelsius = 26.0
                )
            )
        }

        compose.onNodeWithText("30°").assertIsDisplayed()
    }
}
