plugins {
    `kotlin-dsl`
}

group = "com.okensun.todayfeed.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

// One plugin per layer archetype. Each holds its layer's settings AND its dependency
// rules, so a new component inherits the architecture instead of re-declaring it.
gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "todayfeed.jvm"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "todayfeed.android"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "todayfeed.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("quality") {
            id = "todayfeed.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("hilt") {
            id = "todayfeed.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("dataLayer") {
            id = "todayfeed.data"
            implementationClass = "DataLayerConventionPlugin"
        }
        register("uiLayer") {
            id = "todayfeed.ui"
            implementationClass = "UiLayerConventionPlugin"
        }
        register("androidApplication") {
            id = "todayfeed.app"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
