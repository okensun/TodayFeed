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
./gradlew assembleDebug detekt ktlintCheck test                 # what CI runs
./gradlew ktlintFormat                                         # fix formatting
./gradlew :core:testing:test                                   # one module's tests

# Use `test`, never `testDebugUnitTest`. A plain Kotlin module has no debug variant, so
# testDebugUnitTest skips every JVM module without saying so.
./gradlew installDebug                                         # install on a device
```

## Architecture

Component-based Clean Architecture. A **component** is one area of subject matter, not one
screen. Each component owns its own layers, and the build enforces the layering. Not every
component has all four, because a layer with nothing to hold is not created.

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
:components:articles:{api,data,ui}          Spaceflight News
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

Do not check "no Android in `api`" by grepping for `androidx`. `paging-common` resolves to a
plain JVM variant that pulls `androidx.annotation-jvm` and `androidx.arch.core`, neither of which
is Android, so that grep now reports a violation that is not one. The real guarantee is that
`todayfeed.jvm` never applies the Android plugin, so an `android.*` import in an `api` or
`domain` module does not compile. Check that, not the group name.

Also: a grep over a Gradle task's output reports zero when the task itself failed. Check the exit
status, or the answer is "the command broke" wearing the answer "none".

And a build that finishes `in 1s` with everything up to date verified nothing. It means Gradle saw
no changes, not that the code is good. If a file changed after the last real build, run one — or
let CI be the first thing that compiles it, and expect to be told.

### Where screens live

Reading is `:components:feed:ui`. Article detail and Saved are `:components:articles:ui`.
The navigation graph is in `:app`, and routes are `@Serializable` types rather than
strings. Components hand out callbacks such as `onArticleClick(id)` and never a
destination, so no component knows where another component's screens are.

### The four layers are the shape, and a layer with nothing to hold is not created

The table above describes every component. That is the shape to expect. A layer is only
created when there is something to put in it, so a component with no use case has no `domain`
module. The test for `domain` is whether the logic coordinates more than one repository or
source. Smaller logic belongs on the model in `api` or in the `data` mapper. Never add a module
that only passes calls along.

**Never depend on a module you take nothing from.** An unused dependency claims two modules are
related when they are not, and nothing fails while it is wrong, so it can sit there for months.
An empty module is the worst case, because the dependency then also keeps the module alive.

```bash
for d in components/*/*/; do [ -d "${d}src" ] || echo "empty module: $d"; done   # prints nothing
```

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

Create a `domain` module only when there is something to put in it. See the rule above.

## Code style

detekt and ktlint enforce the mechanical rules, so they never belong in a review comment:
140-character lines, four-space indent, no star imports, at most three returns per
function, at most 15 functions per class. Config is in `config/detekt/detekt.yml` and
`.editorconfig`.

- Kotlin sources go in `src/main/kotlin`.
- Packages are `com.okensun.todayfeed.<component>.<layer>`.
- A repository or DAO method starts with a verb, and the verb says which kind of call it is:
  `observe` returns a `Flow`, `find` is a one-shot `suspend` that returns null when there is
  nothing. A bare noun says neither. Do not borrow another component's word either: an article
  repository has no `feed`, because the feed is the mixed list `:components:feed` builds.
- `StateFlow` for state that always has a value, not `SharedFlow(replay = 1)`.
- Collect flows in `repeatOnLifecycle`. Cancel the scope, not child jobs.
- Nothing blocking on the main thread. No expensive work in `init` or in composition.
- Inject dispatchers and `Clock`. Never read the system clock directly, because the
  freshness tests depend on being able to replace it.
- Prefer `?.` over `!!`. Catch specific exceptions, not `Exception`.

### Never write `else` in a `when` over `ContentState`

Write every case out. `ContentState` is a sealed interface, so an exhaustive `when` makes the
compiler stop you when a case is added or when a state starts reaching a screen that did not
handle it before. An `else` throws that away, and the failure it allows is a wrong screen
rather than a crash, so nothing else will catch it either.

This is not something detekt can check, which is why it is here.

The case that keeps being got wrong is `Offline`. It is not a failure: when it carries cached
content, show the content. Only `Offline(null)` is a dead end, and even then it offers retry.

### Comments

Say what the code means now, or warn about this code's own behaviour. Never narrate the
change, never restate the code, and never leave commented-out code. Git records history.

**At most three lines.** A longer comment is not read. If three lines will not carry the
reason, the reason belongs in `DECISIONS.md` and the comment points at it.

## Testing

Hand-written fakes, no mocking library. Shared fakes live in `:core:testing`, including
`FakeClock`, which only moves when a test tells it to. Test names go in backticks and read
as sentences.

The tests that matter most are the ones covering the freshness policy and the cache. They run
on the JVM, with a fake clock and fake sources, so they are fast and repeatable.

**View tests** answer a narrower question: given a state, does the screen draw the right thing
and do its callbacks fire. They use the Compose test rule under Robolectric, so they run in the
ordinary `test` task with no emulator. Write them against the stateless overload of a screen,
never the stateful one, so no Hilt is involved. Use
`androidx.compose.ui.test.junit4.v2.createComposeRule`; the older one is deprecated.

Robolectric emulates the Android level named in `core/testing/src/main/resources/robolectric.properties`,
currently 36. It is pinned because Robolectric has no jar for `compileSdk` 37 yet. Raise it when
one ships.

Gradle's no-tests-discovered check is switched off for `data` and `ui` modules. Turning on
Android resources for unit tests, which Room and Compose tests both need, makes Gradle see test
sources in every module that applies those plugins, so the check fires on modules that simply
have no tests yet. It is meant to catch tests that exist but are not found. Forcing a token test
into a module with nothing worth testing is worse than losing the warning.

One seam is not covered: nothing tests the stateful overload of a screen, which is where the
view model is found and its callbacks are passed down. A screen could stop passing
`viewModel::onRetry` and every view test would still pass. Testing it needs Hilt, which is why
it is left out for now.

**Device behaviour** — back stacks, the system back gesture, a theme change, real loss of
network — is checked by hand over `adb`, and what was measured goes in the README limitations.
There is no emulator in CI.

## Writing style

Every document, comment, commit message and pull request body uses plain, general English
that a reader below high-school English level can follow. Short sentences, one idea each,
common words, active voice. The reviewers are not native English speakers and clear
writing is graded, so simple wording helps.

Commit subjects follow `type(scope): Subject`, where scope is the OpenSpec change name.
The body is at most five lines and never lists files.

## How work lands

Slice 1 went straight to `main`, before this process was agreed. Everything after it goes
through a pull request.

```bash
git switch -c slice/<change-name>          # one branch per OpenSpec change
# implement, one commit per task group
git push -u origin slice/<change-name>
gh pr create --fill                        # body uses .github/pull_request_template.md
# review, fix, push again
gh pr merge --merge                        # a merge commit, never a squash
```

Never squash. The commit history is part of what is being assessed, and a merge commit
also leaves the pull request boundaries visible in `main`.

### Review

A reviewer subagent reads the diff for the branch. It gets the description and the
requirements, and deliberately **not** the session history, so it does not inherit the
author's blind spots. It returns strengths, then issues split into Critical, Important and
Minor, then a clear verdict on whether the branch can merge.

Read `git diff --stat main..HEAD` before writing a pull request body. Four times in one day a
body claimed something its own diff contradicted — "planning only" over three build files,
"covers every state" missing three. The body gets written after the work, by which time what is
in the branch is no longer fresh, so look rather than remember.

Whatever needs fixing is then posted as an **inline comment on the exact line**, two to
three lines long: the problem, then the fix. No summary comment at the top of the pull
request, and no praise-only comments.

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
