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
