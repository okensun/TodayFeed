> **Timebox: about four hours, Sunday evening.** If the convention plugins are not working
> by the two-and-a-half hour mark, take the escape hatch in task 2.8 and move on. This slice
> must not spend the freshness policy's time.

## 1. Gradle foundation

- [x] 1.1 Commit the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), pinned
      to a current Gradle release. Verify: `./gradlew --version` works on a machine with no
      Gradle installed and reports the pinned version
- [x] 1.2 Answer design.md's open question about versions. Look up the current stable AGP,
      Kotlin, Compose BOM, `compileSdk` and `targetSdk` in their real repositories, then put
      them in `gradle/libs.versions.toml`. The Compose BOM decides all Compose versions.
      Verify: `./gradlew help` resolves the catalog with no unresolved dependency
- [x] 1.3 Add `settings.gradle.kts` with plugin management, repository setup,
      `includeBuild("build-logic")` and all twenty-one module declarations. Verify:
      `./gradlew projects` lists every module and the included build
- [x] 1.4 Add the root `build.gradle.kts`, `gradle.properties` (JVM args, AndroidX,
      configuration cache, non-transitive R class) and `.editorconfig` (4-space indent, 140
      characters, no star imports). Verify: `./gradlew help` runs clean with the
      configuration cache on

## 2. Convention plugins that hold the layer rules

- [x] 2.1 Create the `build-logic` included build with its own settings and access to the
      version catalog. No plugins yet. Verify: `./gradlew :build-logic:tasks` works
- [x] 2.2 Add the shared Android setup: `minSdk 24`, the pinned `compileSdk` and
      `targetSdk`, the Java 17 toolchain, and core library desugaring with
      `desugar_jdk_libs`. Verify: a test module using it builds and can call
      `java.time.Instant.now()`
- [x] 2.3 Add the `todayfeed.api` plugin as a plain JVM library that never applies the
      Android plugin. Verify: a module using it fails to compile an `android.*` import. That
      failure is what makes "no Android in api" a build fact
- [x] 2.4 Add the `todayfeed.domain` plugin: JVM library plus coroutines. Verify:
      `./gradlew <module>:dependencies` on a domain module shows no Android artifacts
- [x] 2.5 Add the `todayfeed.data` plugin: Android library and Hilt, plus `:core:network`,
      `:core:database` and `:core:freshness`. Verify: a data module builds without declaring
      those three itself
- [x] 2.6 Add the `todayfeed.ui` plugin: Android library, Compose and Hilt, plus
      `:core:designsystem`. Verify: a ui module compiles a simple `@Composable`, and its
      dependency report holds no `data` module
- [x] 2.7 Add the `todayfeed.core` and `todayfeed.application` plugins. Verify:
      `./gradlew assembleDebug` reaches the app module
- [x] 2.8 Escape hatch. If `build-logic` cannot be made to work in its timebox, fall back to
      plain per-module setup and add it to the deferred list in `docs/ROADMAP.md`. Verify:
      the fallback still passes task 1.4

## 3. Core modules

- [x] 3.1 Create `:core:freshness` as a plain Kotlin module. For now it only exposes an
      injectable `Clock`. Verify: `./gradlew :core:freshness:dependencies` shows no Android,
      no Room and no Retrofit. Slice 2 depends on that staying true
- [x] 3.2 Create `:core:network` with the OkHttp, Retrofit and serialization setup and no
      endpoints. Verify: it builds and exposes a configured client
- [x] 3.3 Create `:core:database` with shared Room settings and an `Instant` type converter,
      and no tables. Verify: it builds and holds no `@Database` class
- [x] 3.4 Create `:core:testing` with a `FakeClock` and coroutine test rules, used only from
      test code. Verify: `./gradlew :core:freshness:dependencies --configuration
      runtimeClasspath` does not list it
- [x] 3.5 Add a unit test that `FakeClock` only moves when told to. Verify: it passes. Every
      freshness test in slice 2 rests on this

## 4. Component modules

- [x] 4.1 Create the four `articles` modules (`api`, `domain`, `data`, `ui`) with the layer
      plugins and placeholder types only. Verify: `assembleDebug` works, and
      `./gradlew :components:articles:ui:dependencies` lists neither
      `:components:articles:data` nor Room
- [x] 4.2 Create the three `weather` modules (`api`, `data`, `ui`). Verify: the same
      dependency check passes
- [x] 4.3 Create `:components:feed:domain`, depending only on the `api` modules of
      `articles` and `weather`. Verify: its dependency report lists no `data` and no `ui` module
- [x] 4.4 Create `:components:feed:ui`, depending on `:components:feed:domain` and the `ui`
      modules of `articles` and `weather`. Verify: it builds, and it is the only `ui` module in the
      project that depends on another component's `ui`
- [x] 4.5 Check all of group 4 at once. Confirm each dependency rule from design.md: no
      module except `:app` depends on a `data` module, no component depends on another
      component's `domain`, and `feed:ui` is the only cross-component `ui` dependency. Write
      the commands down so the check can be repeated later

## 5. Design system

- [x] 5.1 Add the light and dark Material 3 colour schemes based on the reference screens'
      green, with dynamic colour turned off, plus typography and spacing tokens. Verify:
      both schemes show correctly in Compose previews
- [x] 5.2 Add the `TodayFeedTheme` composable. It picks the scheme from the system setting
      and matches the status bar to it. Verify: changing the system setting while the app is
      open switches the theme without a restart
- [x] 5.3 Add the `ContentState` sealed interface: `Loading`, `Empty`, `Error`, `Offline`,
      `Content<T>`. `Offline` is a non-failing state that can carry content. Verify: a unit
      test shows `Offline` can hold content and `Error` cannot
- [x] 5.4 Add one composable per non-content state: loading, empty, error with retry,
      offline with retry. Each must look clearly different from the others. Verify: all four
      render in previews in light and dark, covering the spec scenarios about telling states
      apart, offering retry, and staying readable

## 6. App shell and navigation

- [x] 6.1 Add the `@HiltAndroidApp` application class and the single `@AndroidEntryPoint`
      Compose activity in `:app`. Verify: `assembleDebug` works with Hilt code generation
      and the app starts
- [x] 6.2 Define the `@Serializable` route types in `:app`: the two top-level destinations
      and `ArticleDetail(articleId)`. Verify: a unit test sends `ArticleDetail` through the
      navigation argument encoding and gets the same value back
- [x] 6.3 Add the `NavHost` and the two-tab bottom bar, saving and restoring each tab's back
      stack. Verify by hand the spec's four navigation scenarios, including that tapping the
      current tab adds no back stack entry and that leaving and returning keeps the state
- [x] 6.4 Wire the detail destination. Both the Reading and Saved screens open it through an
      `onArticleClick(id)` callback owned by `:app`, and it closes on both the system back
      gesture and the in-app back button. Verify: the spec's open and return scenarios pass,
      and no component module mentions a route type
- [x] 6.5 Handle an unknown article id on the detail screen by showing the error state with a
      way out. Verify: navigating to a made-up id does not crash the app
- [ ] 6.6 Handle a system theme change while the app is open. Verify: the current destination
      and its state survive the change, as the spec requires.
      NOT VERIFIED. On this emulator `adb shell cmd uimode night` swaps the task and the
      process, which loses the state for reasons that have nothing to do with the app, and
      `settings put secure ui_night_mode` has no effect at all. State does survive an
      equivalent configuration change: after a font scale change the process, the task and
      the scroll position are all unchanged. Needs checking by hand through Settings ->
      Display -> Dark theme, which is the path a real user takes

## 7. Placeholder screens

- [x] 7.1 Add a placeholder `@HiltViewModel` and screen to `:components:feed:ui` that
      exposes a fixed `ContentState` through a `StateFlow`. Verify: it shows under the
      Reading tab and a unit test reads the first state
- [x] 7.2 Add the placeholder Saved screen and its ViewModel to `:components:articles:ui`.
      Verify: it shows under the Saved tab
- [x] 7.3 Add the placeholder detail screen and ViewModel to `:components:articles:ui`,
      reading the article id from `SavedStateHandle`. Verify: a unit test shows the id
      reaches the ViewModel
- [x] 7.4 Add a placeholder hero card composable to the `weather` `ui` module and draw it
      above the article list in the Reading screen. Verify: the feed shows two clearly
      different placeholder cards. This proves the path the real heterogeneous feed will use

## 8. Static analysis, CI and documents

- [x] 8.1 Add detekt with `config/detekt/detekt.yml` (140 characters, class member ordering,
      `MagicNumber` in production code only, at most 3 returns, at most 15 functions) and
      ktlint driven by `.editorconfig`. Verify: `./gradlew detekt ktlintCheck` passes on the
      whole project
- [x] 8.2 Add `.github/workflows/ci.yml`: JDK 17, Gradle caching, and
      `./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest --stacktrace` on push
      and pull request, with no secrets. Verify: the workflow passes on a pushed branch
- [x] 8.3 Write `AGENTS.md`: build commands, the module graph, the dependency rules, the
      layer conventions, code style, writing style and testing patterns. Verify: someone can
      add a new component using only this file
- [x] 8.4 Write `CLAUDE.md` as a short summary that points at `AGENTS.md` and lists only the
      rules a linter cannot catch. Verify: it fits on one screen and does not contradict
      `AGENTS.md`
- [x] 8.5 Add `.github/pull_request_template.md` with the agreed sections. Verify: it renders
      correctly when opening a pull request
- [x] 8.6 Write the `README.md` stub: the one-line run command, a short overview and the
      module graph. Verify: a reader who follows only the README can build the project

- [x] 8.7 Add view tests: the Compose test rule under Robolectric in the `todayfeed.ui`
      convention plugin, Android resources on for unit tests, and the emulated Android level
      pinned in `:core:testing` because Robolectric has no jar for `compileSdk` 37. Verify:
      `./gradlew test` passes with no device, and three of the tests fail against the code as
      it was before the review on pull request 1

## 9. Acceptance

- [ ] 9.1 Check the whole change end to end. Clone the repository into a new directory. Run
      `./gradlew assembleDebug` with no `local.properties` and no environment variables.
      Install the APK. Confirm the app opens on Reading, both tabs switch and keep their
      state, detail opens and returns, and the theme follows the system setting. This covers
      every scenario in the `app-shell` spec
