plugins { id("todayfeed.data") }

android { namespace = "com.okensun.todayfeed.components.weather.data" }

dependencies { implementation(project(":components:weather:api")) }
