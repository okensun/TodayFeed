plugins { id("todayfeed.jvm") }

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(project(":core:freshness"))
}
