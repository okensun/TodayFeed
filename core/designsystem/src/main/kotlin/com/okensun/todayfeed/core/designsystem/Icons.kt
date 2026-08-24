package com.okensun.todayfeed.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The back arrow, drawn here rather than taken from `material-icons-core`. The Compose bill of
 * materials pins that library at 1.7.8 while the rest of Compose is on 1.12, so it is frozen, and
 * one glyph does not pay for it. See DECISIONS.md.
 *
 * `autoMirror` turns the arrow around in a right to left layout, which is what back has to do.
 * The path is filled black because [androidx.compose.material3.Icon] tints it with the content
 * colour of wherever it is drawn.
 *
 * The numbers are one path in a 24 by 24 viewport. Naming each coordinate would hide the shape
 * rather than explain it, which is the opposite of what the rule is for.
 */
@Suppress("MagicNumber")
val BackArrow: ImageVector by lazy {
    val arrow =
        ImageVector.Builder(
            name = "BackArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true
        )
    arrow.path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineTo(13.42f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        verticalLineTo(11f)
        close()
    }
    arrow.build()
}

/**
 * A filled star for a kept article and a hollow one for an unkept article. Drawn here for the
 * same reason as [BackArrow]. The numbers are the Material paths in a 24 by 24 viewport.
 */
@Suppress("MagicNumber")
val SavedStar: ImageVector by lazy {
    val star =
        ImageVector.Builder(
            name = "SavedStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
    star.path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 17.27f)
        lineTo(18.18f, 21f)
        lineToRelative(-1.64f, -7.03f)
        lineTo(22f, 9.24f)
        lineToRelative(-7.19f, -0.61f)
        lineTo(12f, 2f)
        lineTo(9.19f, 8.63f)
        lineTo(2f, 9.24f)
        lineToRelative(5.46f, 4.73f)
        lineTo(5.82f, 21f)
        close()
    }
    star.build()
}

/** The same star drawn as an outline. Stroked rather than filled with a hole, because a hole
 * depends on a fill rule and a stroke does not. */
@Suppress("MagicNumber")
val UnsavedStar: ImageVector by lazy {
    val star =
        ImageVector.Builder(
            name = "UnsavedStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
    star.path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(12f, 17.27f)
        lineTo(18.18f, 21f)
        lineToRelative(-1.64f, -7.03f)
        lineTo(22f, 9.24f)
        lineToRelative(-7.19f, -0.61f)
        lineTo(12f, 2f)
        lineTo(9.19f, 8.63f)
        lineTo(2f, 9.24f)
        lineToRelative(5.46f, 4.73f)
        lineTo(5.82f, 21f)
        close()
    }
    star.build()
}

/**
 * Shown where a picture was meant to be and did not arrive, whether it failed, timed out or the
 * address was wrong. Still, because it is an answer rather than a wait.
 */
@Suppress("MagicNumber")
val NoPicture: ImageVector by lazy {
    val broken =
        ImageVector.Builder(
            name = "NoPicture",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
    broken.path(fill = SolidColor(Color.Black)) {
        moveTo(21f, 5f)
        verticalLineToRelative(6.59f)
        lineToRelative(-3f, -3.01f)
        lineToRelative(-4f, 4.01f)
        lineToRelative(-4f, -4f)
        lineToRelative(-4f, 4f)
        lineToRelative(-3f, -3.01f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        close()
        moveTo(18f, 11.42f)
        lineToRelative(3f, 3.01f)
        verticalLineTo(19f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
        verticalLineToRelative(-6.58f)
        lineToRelative(3f, 2.99f)
        lineToRelative(4f, -4f)
        lineToRelative(4f, 4f)
        close()
    }
    broken.build()
}
