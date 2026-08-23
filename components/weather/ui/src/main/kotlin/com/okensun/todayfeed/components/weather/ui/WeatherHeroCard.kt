package com.okensun.todayfeed.components.weather.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.okensun.todayfeed.components.weather.api.Weather
import kotlin.math.roundToInt

/**
 * A wide hero card pinned above the article list. Its shape is deliberately unlike an
 * article row, which is what makes the feed visibly heterogeneous.
 */
@Composable
fun WeatherHeroCard(
    weather: Weather,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = weather.placeName, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${weather.temperatureCelsius.roundToInt()}°",
                style = MaterialTheme.typography.displayMedium
            )
            Row {
                Text(text = weather.condition, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "  H ${weather.highCelsius.roundToInt()}°  L ${weather.lowCelsius.roundToInt()}°",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
