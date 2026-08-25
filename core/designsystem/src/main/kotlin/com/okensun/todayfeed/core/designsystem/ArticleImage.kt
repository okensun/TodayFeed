package com.okensun.todayfeed.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var state by remember(url) { mutableStateOf(Picture.Waiting) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            Picture.Waiting -> WaitingBlock(Modifier.matchParentSize())
            Picture.Missing -> EmptyBlock(NO_PICTURE, Modifier.matchParentSize())
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
