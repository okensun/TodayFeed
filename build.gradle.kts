// Declared here with `apply false` so the plugin classes are on every project's build
// script classpath. The convention plugins in build-logic depend on them with
// `compileOnly` and apply them at run time, which only works if they resolve here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
