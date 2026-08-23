import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("todayfeed.quality")
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> { configureAndroidLibrary(this) }
        dependencies.add("implementation", library("kotlinx-coroutines-android"))
        dependencies.add("testImplementation", library("junit"))
        dependencies.add("testImplementation", library("kotlinx-coroutines-test"))
        dependencies.add("testImplementation", library("turbine"))
    }
}
