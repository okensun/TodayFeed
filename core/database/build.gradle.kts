plugins { id("todayfeed.android") }

android { namespace = "com.okensun.todayfeed.core.database" }

dependencies {
    api(libs.room.runtime)
    api(libs.room.ktx)
    implementation(project(":core:freshness"))
}
