package com.okensun.todayfeed.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A branded content feed, so the palette is fixed rather than taken from the user's
// wallpaper. Material You dynamic colour is switched off. See DECISIONS.md.
private val BrandGreen = Color(0xFF06C755)
private val BrandGreenDark = Color(0xFF04A344)
private val BrandGreenLight = Color(0xFF7BE8A5)

internal val TodayFeedLightColors =
    lightColorScheme(
        primary = BrandGreenDark,
        onPrimary = Color.White,
        primaryContainer = BrandGreenLight,
        onPrimaryContainer = Color(0xFF00210E),
        secondary = Color(0xFF4F6354),
        onSecondary = Color.White,
        background = Color(0xFFFBFDF8),
        onBackground = Color(0xFF191C1A),
        surface = Color(0xFFFBFDF8),
        onSurface = Color(0xFF191C1A),
        surfaceVariant = Color(0xFFDCE5DC),
        onSurfaceVariant = Color(0xFF404943),
        outline = Color(0xFF707973),
        error = Color(0xFFBA1A1A),
        onError = Color.White
    )

internal val TodayFeedDarkColors =
    darkColorScheme(
        primary = BrandGreen,
        onPrimary = Color(0xFF003919),
        primaryContainer = Color(0xFF005227),
        onPrimaryContainer = BrandGreenLight,
        secondary = Color(0xFFB6CCB9),
        onSecondary = Color(0xFF223527),
        background = Color(0xFF191C1A),
        onBackground = Color(0xFFE1E3DE),
        surface = Color(0xFF191C1A),
        onSurface = Color(0xFFE1E3DE),
        surfaceVariant = Color(0xFF404943),
        onSurfaceVariant = Color(0xFFC0C9C0),
        outline = Color(0xFF8A938C),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    )
