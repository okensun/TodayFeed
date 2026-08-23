## 1. Gradle foundation

- [ ] 1.1 Commit the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) pinned
      to a current Gradle release; verify `./gradlew --version` succeeds on a machine
      with no Gradle installed and reports the pinned version
- [ ] 1.2 Resolve design.md's open question on versions: check the current stable AGP,
      Kotlin, Compose BOM, `compileSdk` and `targetSdk` against their actual
      repositories, and record the chosen values in `gradle/libs.versions.toml` with the
      Compose BOM as the single source of truth for Compose artifact versions; verify
      `./gradlew help` resolves the catalog without an unresolved-dependency error
- [ ] 1.3 Add `settings.gradle.kts` with `pluginManagement`, dependency resolution
      management pointing at the catalog, and `includeBuild("build-logic")`; verify
      `./gradlew projects` lists the included build
- [ ] 1.4 Add root `build.gradle.kts` and `gradle.properties` (JVM args, AndroidX,
      configuration cache, non-transitive R class); verify `./gradlew help` runs clean
      with the configuration cache enabled

## 2. Convention plugins

- [ ] 2.1 Create the `build-logic` included build with its own settings and catalog
      access, exposing no plugins yet; verify `./gradlew :build-logic:tasks` succeeds
- [ ] 2.2 Add the shared Android configuration used by every Android module — `minSdk`
      24, the pinned `compileSdk`/`targetSdk`, Java 17 toolchain, and
      `isCoreLibraryDesugaringEnabled = true` with `desugar_jdk_libs` — and expose it as
      the `todayfeed.android.application` and `todayfeed.android.library` plugins; verify
      a scratch module applying the library plugin assembles
- [ ] 2.3 Add the `todayfeed.android.library.compose` plugin wiring the Compose compiler
      plugin and the Compose BOM; verify a module applying it compiles a trivial
      `@Composable`
- [ ] 2.4 Add the `todayfeed.hilt` and `todayfeed.jvm.library` plugins; verify a module
      applying `todayfeed.hilt` generates Hilt components during `assembleDebug`
- [ ] 2.5 Add the `todayfeed.android.feature` plugin that applies the Compose and Hilt
      conventions and declares the `:core:data` and `:core:designsystem` dependencies
      once; verify a feature module compiles against both without declaring either
      itself
- [ ] 2.6 If `build-logic` cannot be made to work within its timebox, fall back to plain
      per-module configuration and record the fallback in `docs/ROADMAP.md`'s deferred
      list; verify the fallback still satisfies task 1.4

## 3. Module graph

- [ ] 3.1 Create `:core:model` as a plain JVM library with no Android dependency; verify
      `./gradlew :core:model:dependencies` shows no Android artifacts
- [ ] 3.2 Create the empty-but-wired `:core:network`, `:core:database` and `:core:data`
      modules with the dependency directions from design.md and no networking or
      persistence libraries yet; verify `./gradlew :core:data:dependencies` shows
      `:core:network`, `:core:database` and `:core:model` and nothing else from the
      project
- [ ] 3.3 Create `:core:testing` holding shared test fixtures and coroutine test
      helpers, depended on only from test source sets; verify `./gradlew
      :core:data:dependencies --configuration debugRuntimeClasspath` does not contain it
- [ ] 3.4 Create the three feature modules and `:app` using the convention plugins;
      verify `./gradlew assembleDebug` succeeds and that no feature module's dependency
      report lists another feature module, `:core:network` or `:core:database`

## 4. Design system

- [ ] 4.1 Add the light and dark Material 3 colour schemes built on the reference
      screens' green, with dynamic colour explicitly disabled, plus typography and
      spacing tokens; verify both schemes render in Compose previews
- [ ] 4.2 Add the `TodayFeedTheme` composable that selects the scheme from the system
      setting and applies the matching status-bar appearance; verify toggling the system
      setting while the app is foregrounded re-renders in the other appearance without
      restarting
- [ ] 4.3 Add the `ContentState` sealed interface (`Loading`, `Empty`, `Error`,
      `Offline`, `Content<T>`) with `Offline` modelled as a non-failing state that can
      carry content, per design.md; verify the type compiles and a unit test asserts
      `Offline` can hold content while `Error` cannot
- [ ] 4.4 Add one composable per non-content state — loading, empty, error with retry,
      offline with retry — each visually distinguishable from the others; verify the
      four render in previews in both appearances, covering the spec's "each state is
      visually distinguishable", "retry is offered on failure" and legibility scenarios

## 5. App shell and navigation

- [ ] 5.1 Add the `@HiltAndroidApp` application class and the single
      `@AndroidEntryPoint` Compose activity; verify `assembleDebug` succeeds with Hilt
      code generation and the app launches
- [ ] 5.2 Define the `@Serializable` route types — the two top-level destinations and
      `ArticleDetail(articleId)` — in `:app`; verify a unit test round-trips
      `ArticleDetail` through the navigation argument encoding
- [ ] 5.3 Add the `NavHost` and the two-destination bottom bar, with per-destination
      back-stack save and restore; verify the spec's four top-level navigation
      scenarios by hand, including that re-selecting the current destination adds no
      back-stack entry and that leaving and returning restores state
- [ ] 5.4 Wire the detail destination so both features open it via an
      `onArticleClick(id)` callback owned by `:app`, and handle dismissal by system back
      gesture and in-app affordance; verify the spec's opening and returning scenarios
- [ ] 5.5 Handle the detail destination being opened with an unknown identifier by
      showing the error state with a way out; verify the app does not crash when
      navigated to a fabricated identifier

## 6. Feature placeholders

- [ ] 6.1 Add a placeholder `@HiltViewModel` and screen to `:feature:feed` exposing a
      hard-coded `ContentState` through a `StateFlow`; verify the screen renders under
      the Reading destination and a unit test collects the initial state
- [ ] 6.2 Add the equivalent placeholder to `:feature:saved`; verify it renders under
      the Saved destination
- [ ] 6.3 Add the equivalent placeholder to `:feature:detail`, reading the article
      identifier from its `SavedStateHandle`; verify a unit test asserts the identifier
      reaches the ViewModel

## 7. CI, docs and acceptance

- [ ] 7.1 Add `.github/workflows/ci.yml` running JDK 17 with Gradle caching and
      `./gradlew assembleDebug testDebugUnitTest --stacktrace` on push and pull request,
      with no secrets configured; verify the workflow passes on a pushed branch
- [ ] 7.2 Add the `README.md` stub with the one-line run command, a one-paragraph
      overview, and the module graph; verify a reader who follows only the README can
      build the project
- [ ] 7.3 Acceptance check spanning the whole change: clone the repository into a fresh
      directory, run `./gradlew assembleDebug` with no `local.properties` and no
      environment variables set, install the resulting APK, and confirm the app launches
      into Reading, both tabs switch and restore state, detail opens and returns, and
      the appearance follows the system setting — covering every scenario in the
      `app-shell` spec
