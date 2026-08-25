plugins { id("todayfeed.data") }

android { namespace = "com.okensun.todayfeed.components.movie.data" }

dependencies { implementation(project(":components:movie:api")) }
