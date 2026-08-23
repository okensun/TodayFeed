plugins { id("todayfeed.jvm") }

dependencies {
    api(project(":components:articles:api"))
    implementation(project(":core:freshness"))
}
