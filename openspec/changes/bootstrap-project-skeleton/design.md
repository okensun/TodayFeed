## Context

Greenfield repository — there is no existing build, module or code to accommodate. The
constraints that shape this design all come from the brief (see `proposal.md` — Why):
`minSdk 24`, Kotlin, a clean checkout that builds with one command and no secrets, and
later slices that will add an offline-first cache with a per-source freshness policy.

Two of those constraints reach further than they look:

- **`minSdk 24` and time.** The freshness policy in slice 2 is fundamentally about
  timestamps and durations. `java.time` is only available natively from API 26, so
  either the project commits to core library desugaring now or the data layer ends up
  written against `Long` epoch millis and `Calendar`. That is a build-configuration
  decision, which makes it this change's problem, not slice 2's.
- **No secrets required to build.** The candidate source for slice 4's carousel was
  TMDB, which requires an API key. Rather than build a mechanism for optional
  configuration, the source was changed: TVMaze serves the same card shape with no key
  at all. That makes "builds with no configuration" a structural property of the project
  rather than a behaviour it has to implement correctly — which is why it belongs in
  this change's context even though the carousel itself is three slices away.

## Goals / Non-Goals

**Goals:**

- One command (`./gradlew assembleDebug`) turns a clean checkout into an installable
  debug APK, on a machine with only a JDK — no Android Studio, no local configuration,
  no keys.
- A module graph whose dependency rules are enforced by the build, not by discipline.
- Per-module build files short enough that adding a module is a two-line decision.
- The `app-shell` spec's behaviour, in full, against placeholder content.
- CI that fails on a broken build or a failing unit test.

**Non-Goals:**

- No abstraction is introduced for a caller that does not exist yet. `:core:network` and
  `:core:database` are created and wired but empty; their contents are slice 2's design
  problem, and guessing at them now would produce interfaces shaped by imagination
  rather than use.
- No release signing, no ProGuard/R8 rules, no Play Store metadata. The deliverable is a
  repository a reviewer builds, not a shipped app.
- No baseline profiles, no macrobenchmark module, no dependency-analysis or lint-rule
  plugins. Each is defensible in a real codebase and none of them earn a mark here.
- No instrumented tests in CI. An emulator in CI is slow and flaky, and slice 2's tests
  are JVM tests by design.

## Decisions

### The module graph, and the rule that keeps it honest

```
                       :app
                        │  (assembles; owns navigation, no feature logic)
     ┌──────────────────┼──────────────────┐
:feature:feed     :feature:detail     :feature:saved
     └──────────────────┼──────────────────┘
                        ▼
              :core:data  ──────────► :core:network
                  │                    │
                  ├────────────────────┴──► :core:model  (pure Kotlin, no Android)
                  ▼
            :core:database
                        ▲
:core:designsystem ─────┘ (no; designsystem depends only on :core:model)

:core:testing → depended on by test source sets only
```

The rule: **feature modules never depend on each other, and never on `:core:network` or
`:core:database`.** Features see exactly two things — `:core:data` for content and
`:core:designsystem` for how to draw it. Anything shared between two features moves down
into core rather than sideways between features.

This matters more than the tidiness suggests. Feature-to-feature dependencies are how a
modular codebase quietly turns back into a monolith with extra Gradle files, and
`:feature:detail` is the obvious first offender — both feed and saved navigate into it.
Keeping navigation in `:app` is what makes that avoidable: features expose their screens
plus a callback such as `onArticleClick(id)`, and `:app` is the only module that knows
which destination that leads to.

*Alternatives considered.* A single `:app` module with layered packages — genuinely
faster and defensible at this size, rejected in `proposal.md` for the reasons stated
there. Feature modules split further into `:feature:x:ui` / `:feature:x:domain`, as
larger reference apps do — rejected as pure ceremony at three features; the split earns
its keep when several teams share a feature, and there is one of me.

### Convention plugins in an included build, not `buildSrc`

Shared Android/Kotlin/Compose/Hilt configuration lives in `build-logic/`, included via
`includeBuild` in `settings.gradle.kts` and applied as
`todayfeed.android.library`-style plugin ids.

`buildSrc` was the obvious alternative and is rejected because a change to anything in
`buildSrc` invalidates the build-script classpath for the entire project, so every
module recompiles. With ten modules that turns a one-line build tweak into a full
rebuild. An included build is also the pattern the reviewer is most likely to recognise
from Now in Android, which is worth something when someone is reading the repository
under time pressure.

Planned plugins, deliberately few: `android.application`, `android.library`,
`android.library.compose`, `android.feature`, `hilt`, `jvm.library`. `android.feature` is
where the "features depend on `:core:data` and `:core:designsystem`" rule is expressed
once, as actual dependencies, so a new feature module gets it by construction.

*Alternative rejected:* one `todayfeed.android` plugin with boolean flags
(`enableCompose`, `enableHilt`). Fewer files, but flag-driven build logic is harder to
read than several small plugins with obvious names.

### Core library desugaring, decided here for slice 2's benefit

`isCoreLibraryDesugaringEnabled = true` with `desugar_jdk_libs`, set once in the Android
convention plugins. This buys `java.time` — `Instant`, `Duration`, `Clock` — on API 24.

The pay-off is in slice 2: freshness logic written against an injected `Clock` is trivial
to unit test, because a fake clock makes "this entry is 40 minutes old" a one-line
arrangement instead of arithmetic on epoch millis. The cost is roughly 100–200 KB of APK
and one extra dependency.

*Alternatives considered.* `Long` epoch millis throughout — no build cost, but the
freshness tests become harder to read, and readable freshness tests are close to the
centre of what this assignment grades. `kotlinx-datetime` — a cleaner API and
multiplatform-ready, rejected because it *also* needs desugaring on API 24 for its JVM
implementation, so it adds a dependency without removing the one it was meant to avoid.

### Type-safe navigation, with the graph owned by `:app`

A single `NavHost` in `:app`, with `@Serializable` route objects and data classes rather
than string routes with `{argument}` placeholders. `ArticleDetail(val articleId: String)`
is a compile-time contract; `"detail/{id}"` is a runtime one, and the failure mode of the
latter is a crash from a typo in a string.

Top-level destination state — the spec requires each tab to be restored rather than reset
— comes from saving and restoring the back stack per destination, not from keeping two
separate `NavHost`s alive. Two hosts is the other common approach and is rejected because
it makes "open detail from either tab" ambiguous about which host owns the detail entry.

### Four content states, defined once, in `:core:designsystem`

A sealed interface with `Loading`, `Empty`, `Error`, `Offline` and `Content<T>`, plus one
composable per non-content case. Features map their data into it and get consistent
presentation for free — which is what the spec's "states are consistent across
destinations" scenario asks for.

The interesting one is `Offline`, and it is deliberately **not** modelled as a kind of
`Error`. Offline-with-cached-content is a normal, non-failing state in an offline-first
app: the user gets real content plus an honest note that it may be stale, and that is the
behaviour slice 2 needs. Collapsing it into `Error` would force the UI to choose between
showing cached content and telling the truth about it.

What is *not* decided here: how the data layer reports staleness up to the UI. That
contract belongs with the cache that produces it, in slice 2.

*Alternative rejected:* `kotlin.Result` plus a nullable data field per screen. Less code,
but every screen then re-derives "loading with stale content" from a tuple of nullables,
and that is exactly the logic that should exist in one place.

### Brand colours, not dynamic colour

Hand-authored light and dark Material 3 colour schemes built around the green from the
reference screens, with Material You dynamic colour switched off.

This is a branded content product; a feed that recolours itself to match the user's
wallpaper is not what LINE TODAY does. Dynamic colour is the more modern-looking choice
and is rejected on those grounds, with the reasoning recorded rather than left implicit.
Both schemes are authored together so dark mode is a first-class appearance rather than
an inverted afterthought — the spec's legibility requirement applies equally to both.

### No configuration mechanism, because there is nothing to configure

All four data sources — Spaceflight News, Open-Meteo, DummyJSON and TVMaze — are
keyless. The project therefore ships **no** `local.properties` reader, no `BuildConfig`
secret injection, and no environment-variable fallback.

This was not free: the original plan used TMDB for the carousel, which is the
better-known source with the richer catalogue, and it needs a key. The alternatives were
(a) require the reviewer to obtain a key before the app shows anything, (b) build an
optional-configuration path with graceful degradation so the carousel hides itself when
the key is absent, or (c) pick a keyless source. (a) is straightforwardly at odds with
the single-command ground rule. (b) is what a real product would do, and is the one I
would have shipped if the content were irreplaceable — but here it buys a build-config
mechanism, a degraded code path and a paragraph of README caveats, all to display the
same rectangle of poster art. (c) costs a less famous API.

TVMaze also turned out to be the better source for this assignment, not merely the
cheaper one: its schedule endpoint changes *daily*, which gives the freshness policy a
third cadence tier between weather's minutes and articles' hours. The brief hints at two;
having three makes the per-source argument concrete rather than illustrative.

The consequence worth stating: `assembleDebug` on a clean checkout can never fail for a
configuration reason, because there is no configuration. CI re-proves that on every push.

### Placeholder ViewModels are load-bearing

Each feature ships one `@HiltViewModel` holding nothing but a hard-coded state. They
exist so that this change actually proves the annotation processing, the
`@AndroidEntryPoint` activity, and the module wiring all work together end to end. A
skeleton whose DI graph merely compiles because nothing asks anything of it is not a
skeleton worth having.

### CI

One GitHub Actions workflow on push and pull request: JDK 17, Gradle with caching,
`./gradlew assembleDebug testDebugUnitTest --stacktrace`. No emulator, no signing, no
secrets — which also means CI continuously verifies the "builds from a clean checkout
with no configuration" ground rule on every push, rather than us asserting it in a README
and hoping.

## Risks / Trade-offs

- **Convention plugins turn into a time sink.** Version-catalog access from an included
  build and Compose compiler plugin wiring are the usual snags. → Hard limit: if
  `build-logic` is not working, fall back to plain per-module configuration, note it in
  `DECISIONS.md`, and move on. This slice must not eat the freshness policy's budget.
- **Ten modules is a real ongoing cost**, paid on every future slice in wiring and
  navigation indirection. → Accepted deliberately; it is a named nice-to-have and the
  clearest structural signal available. Convention plugins keep the marginal cost of the
  eleventh module near zero.
- **Pinned dependency versions may not be the current ones.** → Versions are verified
  against the actual repositories at implementation time, not written from memory, and
  live in exactly one file so a correction is one edit.
- **Desugaring adds APK size and a moving dependency for a benefit that only lands in
  slice 2.** → Small, bounded, and the alternative pushes the cost into the tests that
  matter most.
- **Four keyless third-party APIs, no contractual uptime between them.** A source that
  is down during the reviewer's build looks like a bug in my code. → This is precisely
  what the offline and error states in the `app-shell` spec are for, and slice 2's
  offline-first cache means a dead source degrades to stale content rather than a blank
  screen. All four were verified responding before being committed to.
- **A walking skeleton can be mistaken for progress.** Ten wired modules showing
  placeholder text is zero user-facing value. → Timeboxed as one slice, with the module
  graph and desugaring decisions being the actual deliverable rather than the placeholder
  screens.

## Open Questions

- Exact pinned versions for AGP, Kotlin, the Compose BOM, `compileSdk` and `targetSdk`.
  Deferred to implementation, where they can be checked against the repositories instead
  of recalled. Nothing above depends on which stable versions they land on.
- Whether the Saved destination keeps a bottom bar at all once it has real content, or
  the reference screens' two-tab bar is the final shape. Cosmetic, does not affect the
  navigation contract in the spec.
