package com.okensun.todayfeed.components.weather.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okensun.todayfeed.components.weather.api.models.Weather
import com.okensun.todayfeed.core.designsystem.EmptyBlock
import com.okensun.todayfeed.core.designsystem.WaitingBlock
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
    HeroCard(modifier) {
        Column {
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

/**
 * The card's place, taken before the weather is known, so the weather arriving changes what is in
 * the card and not where anything sits. Breathing while an answer is still expected, still once
 * there is no connection to bring one.
 *
 * The inner height stands in for the three rows of type the real card holds. A few points out
 * costs a small settle, which is the thing a whole card appearing from nowhere does not do.
 */
@Composable
fun WeatherHeroPlaceholder(
    missing: Boolean,
    modifier: Modifier = Modifier,
) = HeroCard(modifier) {
    val block = Modifier.fillMaxWidth().height(92.dp)
    if (missing) {
        EmptyBlock(description = NO_WEATHER, modifier = block)
    } else {
        WaitingBlock(block.semantics { contentDescription = WEATHER_COMING })
    }
}

/** Shared so the placeholder cannot drift from the card it stands in for. */
@Composable
private fun HeroCard(
    modifier: Modifier,
    content: @Composable () -> Unit,
) = Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
) {
    Column(modifier = Modifier.padding(20.dp)) { content() }
}

// What a screen reader announces where the weather is not yet, and what a test finds. The two
// are different messages on purpose: one is worth waiting for and the other is not.
const val WEATHER_COMING = "Weather is loading"
const val NO_WEATHER = "No weather"
