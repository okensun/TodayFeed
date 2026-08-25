package com.okensun.todayfeed.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The space something will fill once it arrives, held from the first frame so nothing moves when it
 * lands. Breathing, because something is still expected to happen.
 */
@Composable
fun WaitingBlock(modifier: Modifier = Modifier) {
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
    Box(modifier = modifier.background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)))
}

/**
 * Still, and says what is not there, because the waiting is over.
 *
 * The words are the caller's. "No picture" is wrong for a block that was never going to hold one,
 * and a screen reader saying it would be worse than saying nothing.
 */
@Composable
fun EmptyBlock(
    description: String,
    modifier: Modifier = Modifier,
) = Box(
    modifier =
        modifier
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
            .semantics { contentDescription = description },
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = NoPicture,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxSize(0.4f)
    )
}

/** What a screen reader announces where a picture was meant to be, and what a test finds. */
const val NO_PICTURE = "No picture"
