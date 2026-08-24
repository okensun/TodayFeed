package com.okensun.todayfeed.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

/**
 * A picture that takes no room until it has one to show. While it loads, and if it never
 * arrives, the layout is the one without it, so nothing shifts and nothing waits.
 */
@Composable
fun ArticleImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (url.isNullOrBlank()) return
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {},
        error = {}
    )
}
