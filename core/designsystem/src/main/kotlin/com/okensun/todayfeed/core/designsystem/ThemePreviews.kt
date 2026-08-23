package com.okensun.todayfeed.core.designsystem

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a preview twice, once in each theme. The spec requires text to stay readable
 * against its background in both, so every preview should use this rather than a bare
 * `@Preview`.
 */
@Preview(name = "light")
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews
