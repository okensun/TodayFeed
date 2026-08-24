import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure

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

        // Room needs an Android runtime, so its tests run under Robolectric in the ordinary
        // test task rather than on a device. Same reasoning as the view tests.
        extensions.configure<LibraryExtension> {
            testOptions { unitTests { isIncludeAndroidResources = true } }
        }
        dependencies.add("testImplementation", library("robolectric"))
        dependencies.add("testImplementation", library("room-testing"))
        // Turning on Android resources for unit tests makes Gradle see test sources in every
        // module that applies this plugin, so its no-tests-discovered check fires on modules
        // that simply have no tests yet. The check is meant to catch tests that exist but are
        // not found, which is not this, and forcing a token test into a module with nothing
        // worth testing is worse than the warning.
        // Turning on Android resources for unit tests makes Gradle see test sources in every
        // module that applies this plugin, so its no-tests-discovered check fires on modules that
        // simply have none yet. Switch it off only for those: a module that does have tests keeps
        // the guard, so a configuration change that stops them being found still fails the build.
        val hasTestSources = file("src/test").walkTopDown().any { it.extension == "kt" }
        tasks.withType(Test::class.java).configureEach {
            failOnNoDiscoveredTests.set(hasTestSources)
        }

    }
}
