# AGENTS.md — TodayFeed

Instructions for AI coding agents working in this repository. `CLAUDE.md` is a short
digest of the rules a linter cannot catch; this file is the full guide.

## What this project is

TodayFeed is a content feed app in the style of LINE TODAY. One scrollable list mixes
article cards with a weather hero card. The user can open an article, save it, and read
saved articles with no network.

It is a take-home assessment, so the reasoning matters as much as the code. `DECISIONS.md`
records every significant choice with what was turned down and why. `docs/ROADMAP.md`
holds the plan and what was cut.

## Build and environment

You need a JDK 17 and the Android SDK. Nothing else. There are no API keys, no
`local.properties` entries to invent, and no secrets. Every data source is keyless.

Point Gradle at your SDK by setting `ANDROID_HOME`, or let Android Studio write
`local.properties` for you. `compileSdk` is 37 and Gradle downloads that platform on
first build.

```bash
./gradlew assembleDebug                                       # build the debug APK
./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest   # what CI runs
./gradlew ktlintFormat                                         # fix formatting
./gradlew :core:testing:test                                   # one module's tests
./gradlew installDebug                                         # install on a device
```

## Architecture

Component-based Clean Architecture. A **component** is one area of subject matter, not one
screen. Each component owns its own layers, and the build enforces the layering.

| Layer | Holds | May depend on |
|---|---|---|
| `api` | the repository interfaces, and the models they speak in. Plain Kotlin, no Android | nothing else in the project |
| `domain` | use cases and rules. Plain Kotlin | its own `api`, `:core:freshness` |
| `data` | Retrofit and Room code implementing the `api` interfaces | its own `api`, `:core:network`, `:core:database`, `:core:freshness` |
| `ui` | Compose cards and screens, plus ViewModels | its own `api` and `domain`, `:core:designsystem` |

### Modules

```
:app                              navigation graph, bottom bar, Hilt aggregation
:core:designsystem                theme (light and dark), ContentState, state composables
:core:network                     OkHttp, Retrofit and serialization setup. No endpoints.
:core:database                    shared Room settings and type converters. No tables.
:core:freshness                   the per-source freshness policy. Plain Kotlin.
:core:testing                     FakeClock, shared fakes, coroutine test rules
:components:articles:{api,domain,data,ui}   Spaceflight News
:components:weather:{api,data,ui}           Open-Meteo
:components:feed:{domain,ui}                puts the feed together, owns the Reading screen
```

### What goes in `api`

`api` exists for the **repository interface**, not for the model. The model is only the
vocabulary that interface speaks in.

```
components/articles/api/
  Article.kt              the model
  ArticleRepository.kt     <-- the reason this module exists
```

Without the interface there is nothing for `ui` and `domain` to call, so they would have to
call into `data`, and rule 1 below would be impossible. With it:

```
ui / domain  --depends on-->  api (interface)  <--implements--  data
                                                     ^
                                          :app binds one to the other
```

A quick way to tell whether you have put something in the right place: if `data` is the only
module that could ever use it, it belongs in `data`. If `ui` or `domain` needs to name it, it
belongs in `api`.

Repository interfaces go in `api` and not in `domain`, even though classic Clean
Architecture puts them in the inner circle. See `DECISIONS.md` for why.

### The two rules

1. **Only `:app` may depend on a `data` module.** Not even a component's own `ui` or
   `domain`. So a ViewModel cannot reach Retrofit or a DAO, because those classes are not
   on its build path.
2. **A component may see another component only through its `api` module.** The one
   exception is `:components:feed:ui`, which depends on the other components' `ui` modules
   because drawing their cards in one list is its whole job.

Check both at any time:

```bash
grep -rn "components:[a-z]*:data" --include=build.gradle.kts .    # only app should match
./gradlew :components:articles:ui:dependencies --configuration debugCompileClasspath \
  | grep -iE "room|retrofit"                                      # should print nothing
```

### Where screens live

Reading is `:components:feed:ui`. Article detail and Saved are `:components:articles:ui`.
The navigation graph is in `:app`, and routes are `@Serializable` types rather than
strings. Components hand out callbacks such as `onArticleClick(id)` and never a
destination, so no component knows where another component's screens are.

### A `domain` module only when logic has no model to own it

Only `articles` and `feed` have one. The test is whether the logic coordinates more than
one repository or source. Smaller logic belongs on the model in `api` or in the `data`
mapper. Do not add a `domain` module that only passes calls along.

## Convention plugins

Shared build setup lives in `build-logic`, an included build. One plugin per layer, and
each holds its layer's dependency rules so a new component inherits them.

| Plugin | For | Gives it |
|---|---|---|
| `todayfeed.jvm` | `api`, `domain`, `:core:freshness`, `:core:testing` | plain Kotlin library, coroutines, JUnit. Never applies the Android plugin |
| `todayfeed.android` | `:core:network`, `:core:database` | Android library, desugaring, Java 17 |
| `todayfeed.android.compose` | `:core:designsystem` | the above plus Compose |
| `todayfeed.data` | any `data` module | Android, Hilt, serialization, Room, and the three core modules |
| `todayfeed.ui` | any `ui` module | Compose, Hilt, `:core:designsystem` |
| `todayfeed.app` | `:app` | application, Compose, Hilt, navigation |
| `todayfeed.quality` | applied automatically | detekt and ktlint |

### Adding a component

1. Create `components/<name>/{api,data,ui}` with a `build.gradle.kts` in each.
2. Apply `todayfeed.jvm` in `api`, `todayfeed.data` in `data`, `todayfeed.ui` in `ui`.
3. Set `android { namespace = "com.okensun.todayfeed.components.<name>.<layer>" }` in the
   Android ones.
4. Register all three in `settings.gradle.kts`.
5. Bind the data implementations from `:app`, which is the only module allowed to see them.

Add a `domain` module only if the test above says you need one.

## Code style

detekt and ktlint enforce the mechanical rules, so they never belong in a review comment:
140-character lines, four-space indent, no star imports, at most three returns per
function, at most 15 functions per class. Config is in `config/detekt/detekt.yml` and
`.editorconfig`.

- Kotlin sources go in `src/main/kotlin`.
- Packages are `com.okensun.todayfeed.<component>.<layer>`.
- `StateFlow` for state that always has a value, not `SharedFlow(replay = 1)`.
- Collect flows in `repeatOnLifecycle`. Cancel the scope, not child jobs.
- Nothing blocking on the main thread. No expensive work in `init` or in composition.
- Inject dispatchers and `Clock`. Never read the system clock directly, because the
  freshness tests depend on being able to replace it.
- Prefer `?.` over `!!`. Catch specific exceptions, not `Exception`.

### Comments

Say what the code means now, or warn about this code's own behaviour. Never narrate the
change, never restate the code, and never leave commented-out code. Git records history.

## Testing

Hand-written fakes, no mocking library. Shared fakes live in `:core:testing`, including
`FakeClock`, which only moves when a test tells it to. Test names go in backticks and read
as sentences.

The tests that matter most are the ones covering the freshness policy and the cache. They
run on the JVM, with a fake clock and fake sources, so they are fast and repeatable.

## Writing style

Every document, comment, commit message and pull request body uses plain, general English
that a reader below high-school English level can follow. Short sentences, one idea each,
common words, active voice. The reviewers are not native English speakers and clear
writing is graded, so simple wording helps.

Commit subjects follow `type(scope): Subject`, where scope is the OpenSpec change name.
The body is at most five lines and never lists files.

## Common pitfalls

- **AGP 9 applies Kotlin itself.** Applying `org.jetbrains.kotlin.android` fails the
  build. The Compose compiler plugin is still required, even though the release notes say
  it is not.
- **AGP 9 removed the generic parameters from `CommonExtension`.** Convention plugins copied
  from older guides will not compile. Configure `ApplicationExtension` and
  `LibraryExtension` separately.
- **KSP has no release for Kotlin 2.4.** Room and Hilt both need KSP, so Kotlin stays on
  2.3.21. Do not "upgrade" it.
- **`Plugin.apply` must return `Unit`.** With an expression body that ends in
  `dependencies.add(...)` you need an explicit `: Unit`.
- **Kotlin will not smart cast a public property from another module.** Bind it to a local
  first.
- **A module directory must exist before `settings.gradle.kts` includes it**, or Gradle 9
  fails while reading settings.
