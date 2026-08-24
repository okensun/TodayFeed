plugins {
    id("todayfeed.android")
    // This module owns the client, so it provides it. Keeping that here rather than in :app
    // stops the application module accumulating knowledge of how every core module is set up.
    id("todayfeed.hilt")
}

android {
    namespace = "com.okensun.todayfeed.core.network"
    // So this module can tell a debug build from a release one without being told.
    buildFeatures { buildConfig = true }
}

dependencies {
    // Connectivity is declared in :core:freshness and read here, because reading the platform is
    // this module's job. Pass 5 replaces the stand-in with the real NetworkCapabilities read.
    api(project(":core:freshness"))
    api(libs.retrofit.core)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
}
