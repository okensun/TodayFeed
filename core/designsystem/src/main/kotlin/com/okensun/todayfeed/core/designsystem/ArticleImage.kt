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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var state by remember(url) { mutableStateOf(Picture.Waiting) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            Picture.Waiting -> Waiting(Modifier.matchParentSize())
            Picture.Missing -> Missing(Modifier.matchParentSize())
            Picture.Shown -> Unit
        }
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onState = {
                state =
                    when (it) {
                        is AsyncImagePainter.State.Success -> Picture.Shown
                        // A timeout, a refusal and a wrong address all arrive here, and all of
                        // them mean the same thing to a reader.
                        is AsyncImagePainter.State.Error -> Picture.Missing
                        else -> Picture.Waiting
                    }
            }
        )
    }
}

private enum class Picture {
    Waiting,
    Missing,
    Shown,
}

/** Still, and says what happened, because the waiting is over. */
@Composable
private fun Missing(modifier: Modifier = Modifier) =
    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
                .semantics { contentDescription = NO_PICTURE },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = NoPicture,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxSize(0.4f)
        )
    }

/** Breathing, because something is still expected to happen. */
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

/** What a screen reader announces where a picture was meant to be, and what a test finds. */
const val NO_PICTURE = "No picture"
