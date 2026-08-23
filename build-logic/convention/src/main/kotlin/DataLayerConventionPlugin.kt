import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A data layer module. It gets the three shared infrastructure modules once, here, so no
 * data module has to declare them. Nothing except :app may depend on a data module.
 */
class DataLayerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("todayfeed.android")
        pluginManager.apply("todayfeed.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies.add("implementation", project(":core:network"))
        dependencies.add("implementation", project(":core:database"))
        dependencies.add("implementation", project(":core:freshness"))
        dependencies.add("implementation", library("retrofit-core"))
        dependencies.add("implementation", library("kotlinx-serialization-json"))
        dependencies.add("implementation", library("room-runtime"))
        dependencies.add("implementation", library("room-ktx"))
        dependencies.add("ksp", library("room-compiler"))
        dependencies.add("implementation", library("room-paging"))
        dependencies.add("api", library("paging-runtime"))
        dependencies.add("testImplementation", project(":core:testing"))
    }
}
