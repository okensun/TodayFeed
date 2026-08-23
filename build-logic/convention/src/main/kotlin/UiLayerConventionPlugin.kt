import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
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
        dependencies.add("implementation", library("paging-compose"))
        dependencies.add("testImplementation", library("paging-testing"))
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
        // Turning on Android resources for unit tests makes Gradle see test sources in every
        // module that applies this plugin, so its no-tests-discovered check fires on modules
        // that simply have no tests yet. The check is meant to catch tests that exist but are
        // not found, which is not this, and forcing a token test into a module with nothing
        // worth testing is worse than the warning.
        tasks.withType(Test::class.java).configureEach { failOnNoDiscoveredTests.set(false) }

    }
}
