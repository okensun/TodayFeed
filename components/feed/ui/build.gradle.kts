plugins { id("todayfeed.ui") }

android { namespace = "com.okensun.todayfeed.components.feed.ui" }

dependencies {
    implementation(project(":components:feed:domain"))
    // The one sanctioned cross-component ui dependency: drawing their cards is this
    // module's entire job. See DECISIONS.md.
    implementation(project(":components:articles:ui"))
    implementation(project(":components:weather:ui"))
}
