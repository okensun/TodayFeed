import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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

        // View tests: does this composable draw the right thing for a given state, and do
        // its callbacks fire. Robolectric runs them on the JVM, so they join the ordinary
        // test task and CI needs no emulator.
        extensions.configure<LibraryExtension> {
            testOptions { unitTests { isIncludeAndroidResources = true } }
        }
        val bom = dependencies.platform(library("compose-bom"))
        dependencies.add("testImplementation", bom)
        dependencies.add("testImplementation", library("compose-ui-test-junit4"))
        dependencies.add("debugImplementation", library("compose-ui-test-manifest"))
        dependencies.add("testImplementation", library("robolectric"))
    }
}
