package com.okensun.todayfeed.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun TodayFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TodayFeedDarkColors else TodayFeedLightColors,
        content = content,
    )
}

object Spacing {
    val none = 0
    val small = 8
    val medium = 16
    val large = 24
}
