import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.version(alias: String): String =
    libs.findVersion(alias).get().requiredVersion

internal fun Project.library(alias: String) = libs.findLibrary(alias).get()

/**
 * AGP 9 removed the generic parameters from `CommonExtension`, so the usual
 * `CommonExtension<*, *, *, *, *, *>` helper no longer compiles. Application and library
 * extensions are configured separately instead. The small duplication is cheaper than
 * reintroducing the generics.
 */
internal fun Project.configureAndroidLibrary(extension: LibraryExtension) = with(extension) {
    compileSdk = version("compileSdk").toInt()
    defaultConfig {
        minSdk = version("minSdk").toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Gives java.time on API 24, which is what makes the freshness policy testable
        // against an injected Clock. See DECISIONS.md.
        isCoreLibraryDesugaringEnabled = true
    }
    addDesugaring()
}

internal fun Project.configureAndroidApplication(extension: ApplicationExtension) = with(extension) {
    compileSdk = version("compileSdk").toInt()
    defaultConfig {
        minSdk = version("minSdk").toInt()
        targetSdk = version("targetSdk").toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    addDesugaring()
}

private fun Project.addDesugaring() = dependencies {
    add("coreLibraryDesugaring", library("desugar-jdk-libs"))
}

internal fun Project.configureKotlinJvm() {
    extensions.getByType<JavaPluginExtension>().toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
    }
    extensions.getByType<KotlinJvmProjectExtension>().compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

internal fun Project.addComposeDependencies() {
    val bom = dependencies.platform(library("compose-bom"))
    dependencies.add("implementation", bom)
    dependencies.add("androidTestImplementation", bom)
    listOf("compose-ui", "compose-ui-graphics", "compose-ui-tooling-preview", "compose-material3")
        .forEach { dependencies.add("implementation", library(it)) }
    dependencies.add("debugImplementation", library("compose-ui-tooling"))
}
