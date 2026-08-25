plugins { id("todayfeed.ui") }

android { namespace = "com.okensun.todayfeed.components.feed.ui" }

dependencies {
    implementation(project(":components:feed:domain"))
    // Named directly by the previews and the card calls, so declared rather than relied on
    // through feed:domain's api() declarations.
    implementation(project(":components:articles:api"))
    implementation(project(":components:weather:api"))
    // The one sanctioned cross-component ui dependency: drawing their cards is this
    // module's entire job. See DECISIONS.md.
    implementation(project(":components:articles:ui"))
    implementation(project(":components:movie:ui"))
    implementation(project(":components:weather:ui"))
}
