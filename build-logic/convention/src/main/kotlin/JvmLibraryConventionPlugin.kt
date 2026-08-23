import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plain Kotlin library. The Android plugin is deliberately never applied here, which is what
 * makes "no Android in an api or domain module" a build fact rather than a promise.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("todayfeed.quality")
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlinJvm()
        dependencies.add("implementation", library("kotlinx-coroutines-core"))
        // @Inject on a domain class, without the domain layer depending on Hilt.
        dependencies.add("api", library("javax-inject"))
        dependencies.add("testImplementation", library("junit"))
        dependencies.add("testImplementation", library("kotlinx-coroutines-test"))
        dependencies.add("testImplementation", library("turbine"))
    }
}
