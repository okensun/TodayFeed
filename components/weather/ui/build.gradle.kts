plugins { id("todayfeed.ui") }

android { namespace = "com.okensun.todayfeed.components.weather.ui" }

dependencies { implementation(project(":components:weather:api")) }
