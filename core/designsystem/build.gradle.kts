plugins { id("todayfeed.android.compose") }

android { namespace = "com.okensun.todayfeed.core.designsystem" }

dependencies {
    // The composable lives here; which client fetches the bytes is decided in :app, because this
    // module may not see :core:network.
    implementation(libs.coil.compose)
}
