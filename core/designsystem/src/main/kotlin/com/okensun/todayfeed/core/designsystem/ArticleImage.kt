package com.okensun.todayfeed.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * An article with a picture holds its place from the first frame, so nothing moves when the
 * picture lands. An article with no picture at all draws nothing, which is known at once and
 * therefore costs no movement either.
 */
@Composable
fun ArticleImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (url.isNullOrBlank()) return
    var loaded by remember(url) { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (!loaded) {
            Waiting(Modifier.matchParentSize())
        }
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onState = { loaded = it is AsyncImagePainter.State.Success }
        )
    }
}

/**
 * Breathing while it waits, and still when it has given up. A picture that never arrives leaves
 * this behind rather than collapsing, because a row that changes shape late is worse than a
 * quiet grey square.
 */
@Composable
private fun Waiting(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "waiting").animateFloat(
        initialValue = 0.10f,
        targetValue = 0.28f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
        label = "alpha"
    )
    Box(
        modifier =
            modifier.background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    )
}
