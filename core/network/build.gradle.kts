plugins { id("todayfeed.android") }

android { namespace = "com.okensun.todayfeed.core.network" }

dependencies {
    api(libs.retrofit.core)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
}
