plugins { id("todayfeed.ui") }

android { namespace = "com.okensun.todayfeed.components.articles.ui" }

dependencies {
    implementation(project(":components:articles:api"))
    implementation(project(":components:articles:domain"))
}
