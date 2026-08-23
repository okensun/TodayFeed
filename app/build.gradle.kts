plugins { id("todayfeed.app") }

android {
    namespace = "com.okensun.todayfeed"
    defaultConfig {
        applicationId = "com.okensun.todayfeed"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":components:feed:ui"))
    implementation(project(":components:articles:ui"))
    implementation(project(":components:weather:ui"))
    // Only :app may see a data module. This is where implementations are bound to the
    // interfaces every other module compiles against.
    implementation(project(":components:articles:data"))
    implementation(project(":components:weather:data"))
}
