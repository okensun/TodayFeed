plugins { id("todayfeed.jvm") }

dependencies {
    api(project(":components:articles:api"))
    api(project(":components:weather:api"))
    implementation(project(":core:freshness"))
}
