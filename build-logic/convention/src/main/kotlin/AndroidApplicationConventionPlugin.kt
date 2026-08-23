import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("todayfeed.quality")
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply("todayfeed.hilt")
        extensions.configure<ApplicationExtension> {
            configureAndroidApplication(this)
            buildFeatures { compose = true }
        }
        addComposeDependencies()
        dependencies.add("implementation", library("androidx-activity-compose"))
        dependencies.add("implementation", library("androidx-navigation-compose"))
        dependencies.add("implementation", library("kotlinx-serialization-json"))
        dependencies.add("testImplementation", library("junit"))
    }
}
