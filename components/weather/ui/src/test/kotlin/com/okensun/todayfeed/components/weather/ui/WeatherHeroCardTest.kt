package com.okensun.todayfeed.components.weather.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.okensun.todayfeed.components.weather.api.Weather
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
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
        show(temperature = 30.4)

        compose.onNodeWithText("Taipei").assertIsDisplayed()
        compose.onNodeWithText("30°").assertIsDisplayed()
        compose.onNodeWithText("Cloudy").assertIsDisplayed()
        compose.onNodeWithText("  H 31°  L 26°").assertIsDisplayed()
    }

    @Test
    fun `rounds to the nearest degree, not toward zero`() {
        show(temperature = 30.9)

        compose.onNodeWithText("31°").assertIsDisplayed()
    }

    /**
     * Open-Meteo returns temperatures below zero. Truncating toward zero would round those
     * the wrong way, showing -3 for -3.7.
     */
    @Test
    fun `rounds a temperature below zero away from zero`() {
        show(temperature = -3.7)

        compose.onNodeWithText("-4°").assertIsDisplayed()
    }

    private fun show(temperature: Double) =
        compose.setContent {
            TodayFeedTheme {
                WeatherHeroCard(
                    Weather(
                        placeName = "Taipei",
                        temperatureCelsius = temperature,
                        condition = "Cloudy",
                        highCelsius = 31.0,
                        lowCelsius = 26.0
                    )
                )
            }
        }
}
