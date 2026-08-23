import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A ui layer module: Compose, Hilt and the design system. It never receives a data module,
 * so a ViewModel here cannot reach Retrofit or a DAO even by accident.
 */
class UiLayerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("todayfeed.android.compose")
        pluginManager.apply("todayfeed.hilt")
        dependencies.add("implementation", project(":core:designsystem"))
        dependencies.add("implementation", library("androidx-lifecycle-runtime-compose"))
        dependencies.add("implementation", library("androidx-lifecycle-viewmodel-compose"))
        dependencies.add("implementation", library("androidx-hilt-navigation-compose"))
        dependencies.add("testImplementation", project(":core:testing"))
    }
}
