pluginManagement {
    // Convention plugins live in an included build, not buildSrc. A change in buildSrc
    // invalidates every module's build script classpath. See DECISIONS.md.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TodayFeed"

include(":app")

include(":core:designsystem")
include(":core:network")
include(":core:database")
include(":core:freshness")
include(":core:testing")

// Each component owns its layers. Only :app may depend on a data module, and a component
// sees another component only through its api module. See AGENTS.md for the full rules.
include(":components:articles:api")
include(":components:articles:data")
include(":components:articles:ui")

include(":components:movie:api")
include(":components:movie:data")
include(":components:movie:ui")

include(":components:weather:api")
include(":components:weather:data")
include(":components:weather:ui")

include(":components:feed:domain")
include(":components:feed:ui")
