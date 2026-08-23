plugins { id("todayfeed.data") }

android { namespace = "com.okensun.todayfeed.components.articles.data" }

dependencies { implementation(project(":components:articles:api")) }
