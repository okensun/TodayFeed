plugins { id("todayfeed.jvm") }

dependencies {
    api(project(":components:weather:api"))
    implementation(project(":core:freshness"))
    testImplementation(project(":core:testing"))
}
