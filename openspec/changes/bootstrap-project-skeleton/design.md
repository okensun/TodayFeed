## Context

This is a new repository. There is no existing build, module or code to work around. The
limits that shape this design come from the brief (see `proposal.md` — Why): `minSdk 24`,
Kotlin, a fresh copy that builds with one command and no secrets, and later slices that add
an offline-first cache with a per-source freshness policy over four data sources that
change at different speeds.

The structure follows the shape of a large production Android codebase that is organised as
components, each with its own Clean Architecture layers. Only the shape is borrowed. No
text or code is copied. Two places where that codebase's reasons do not apply here are
listed below as decisions, not as defaults I inherited.

Three limits matter more than they first look:

- **`minSdk 24` and time handling.** The freshness policy is mostly about timestamps and
  durations. `java.time` only exists on API 26 and above. So either the project turns on
  core library desugaring now, or the data layer is written with `Long` millisecond values.
  That is a build setting, so it is this change's problem, not slice 2's.

- **No secrets needed to build.** The first choice for the carousel was TMDB, which needs
  an API key. Instead of building a way to handle an optional key, I changed the source. The
  Studio Ghibli film API gives the same kind of card and needs no key. So "builds with no
  setup" becomes a fact about the project rather than a feature we must get right.

- **Four sources, four different update speeds.** Weather changes every few minutes.
  Articles change every hour or so. The service card list and the film list barely change
  at all, so the right policy for them is closer to "fetch once and keep it". One single
  refresh interval cannot serve all four. Too short and the app wastes the user's mobile
  data re-downloading a film list that has not changed since 1986. Too long and the
  weather card is wrong. That is why the policy is *per-source*, and why it gets its own
  module instead of living inside whichever repository needed it first. The film list is
  the useful extreme: its correct time-to-live is "effectively forever", which is only
  expressible if the policy is a value per source rather than one number for the app.

## Goals / Non-Goals

**Goals:**

- One command (`./gradlew assembleDebug`) turns a fresh copy into an installable debug APK
  on a machine that only has a JDK. No Android Studio, no setup, no keys.
- Dependency rules that the build enforces, not the code review.
- Module build files short enough that adding a module is a two-line decision.
- All the behaviour in the `app-shell` spec, using placeholder content.
- CI that fails on a broken build, a style problem, or a failing unit test.

**Non-Goals:**

- No abstraction for a caller that does not exist yet. `:core:network`, `:core:database`
  and `:core:freshness` are created and wired but empty. What goes in them is slice 2's
  design problem. Guessing now would give us interfaces shaped by imagination instead of
  by real use.
- No release signing, no R8 rules, no Play Store files. The deliverable is a repository a
  reviewer builds.
- No baseline profiles, no benchmark module, no custom lint rules. Each is reasonable in a
  real codebase and none of them earn a mark here.
- No device tests in CI. An emulator in CI is slow and unreliable, and the tests that
  matter in slice 2 run on the JVM anyway.

## Decisions

### Components own their layers, and the build enforces it

A *component* is one area of subject matter, not one screen. Each component owns up to four
layers:

| Layer | What it holds | What it may depend on |
|---|---|---|
| `api` | models and interfaces. Plain Kotlin, no Android | nothing else in the project |
| `domain` | use cases and rules. Plain Kotlin | its own `api`, `:core:freshness` |
| `data` | Retrofit and Room code that implements the `api` interfaces | its own `api`, `:core:network`, `:core:database`, `:core:freshness` |
| `ui` | Compose cards and screens, plus ViewModels | its own `api` and `domain`, `:core:designsystem` |

The first rule: **only `:app` may depend on a `data` module.** A component's `ui` and
`domain` build against the interfaces in `api`. `:app` is the only module that connects the
real implementations to those interfaces. So `:components:articles:ui` *cannot* use Retrofit
or a DAO. Not because we agreed not to, but because those classes are not on its build path.

The second rule: **a component may only see another component through its `api` module.**
No component depends on another component's `domain`, `data` or `ui`. There is one allowed
exception, covered further down.

The modules are worth it because they turn two common review comments into build errors.
"This ViewModel is talking to Room directly" and "this feed use case is reaching into the
weather cache" are the two usual ways a layered Android codebase goes bad. Here, neither can
be written.

*Other options I looked at.* One `:app` module with layered packages: rejected in
`proposal.md`. One global three-layer split: also rejected there, and that is the more
useful rejection, because it keeps the layers but loses the component boundary. Putting the
interfaces and their implementations in the same module: rejected because it only works as
long as nobody imports the implementation, and a module boundary removes the need to trust
that.

### A `domain` module exists only when logic has no model to belong to

Only `articles` and `feed` get a `domain` module. The test is not "does this component have
any logic". All five have some. The test is:

> A `domain` module exists when there is logic that no single model can own.

That means logic which coordinates more than one repository, or which decides something
using more than one source. Anything smaller has a better home.

| Component | Its logic | Where that logic belongs | `domain`? |
|---|---|---|---|
| `weather` | weather code to a weather condition | normalising one model, so the `data` mapper | no |
| `serviceCard` | price after discount | arithmetic on one model, so a computed property in `api` | no |
| `movie` | sort by air time, drop items with no image, "on now" or "later today" | a pure function over one model plus the clock, so `api` | no |
| `articles` | move the paging cursor, make save and unsave work with the cache, decide from freshness whether to call the network | coordination across repositories. No model owns it | **yes** |
| `feed` | combine four sources, choose the section order, decide what to show when one source fails | a decision across sources | **yes** |

The `api` modules are plain Kotlin, so a model is allowed to carry its own arithmetic. Putting
`discountedPrice` on the `Product` model in `api` is more honest than opening a module for it.

Applying all four layers to every component would have added modules that only pass calls
along, plus one that nobody imports. That is what happens when you copy a structure instead of
following the idea behind it.

**Scope note.** The deadline moved to Thursday, leaving about sixteen working hours. Only
`articles`, `weather` and `feed` are built. `movie` is designed and added last if there is
time. `serviceCard` is cut. The heterogeneous feed requirement needs articles plus one more
source, and `weather` is that source, so cutting the other two leaves every must-have intact.
See `docs/ROADMAP.md` for the full cut list.

This test is worth stating because it can be checked. "Does anything here need to coordinate
more than one source?" has an answer. "Is there enough logic to justify a module?" does not.

*Option rejected:* give every component the same four layers, on the grounds that one shape is
easier to learn and an empty layer only costs a build file. That is fair, but a pass-through
class is worse than a small inconsistency. It invites logic that belongs somewhere else, and it
makes the `articles` domain module look like boilerplate instead of the one place the rules
live.

### Every component keeps its own Room database

`:core:database` holds shared Room settings and type converters, such as the one for
`Instant`. It holds no tables. Each component's `data` module declares its own
`RoomDatabase` with its own tables and DAOs.

The reason is a dependency cycle. If `:core:database` owned the `@Database` class, it would
have to name every table, so it would depend on each component's `data` module. Those
modules already depend on it. There are three ways out: move all tables into
`:core:database` (what Now in Android does), declare the `@Database` in `:app`, or give each
component its own database.

One database per component wins for more than breaking the cycle. There is no shared schema,
so there is no shared migration risk. Changing how articles are stored cannot break the
weather cache. Each component's storage can also be tested on its own with an in-memory
database. The cost is four small database files and no way to write a query across
components. No such query exists: the feed is put together in memory from four independent
sources, which is what `:components:feed:domain` is for.

*Other options.* All tables in `:core:database`: the best-known option and a fine default,
rejected because it takes ownership of storage away from the component that owns the data,
and that ownership is the point of the whole layout. `@Database` in `:app`: works, but it
puts schema and migration decisions in the module least able to reason about them.

### `:components:feed:ui` is the one allowed cross-component UI dependency

`:components:feed:ui` depends on the `ui` modules of `articles`, `weather`, `serviceCard` and
`movie`, because drawing their cards in one list is its whole purpose. Each of those
modules gives out one stateless card composable. `feed:ui` owns the list, the scroll position
and the single ViewModel behind it.

Card composables take `api` models directly. The other option is for each component to
publish its own UI model plus a mapper, and for `feed:ui` to map into it. That adds one type
and one mapper per card type, to buy the same protection the `api` boundary already gives.
The `api` models are already framework-free and already the shared language between modules.

Making `feed` a component instead of a screen inside `:app` is what keeps this contained. The
cross-component knowledge sits in exactly one module whose name says so, instead of piling
up in the application module.

*Option rejected:* put every card composable in `feed:ui`, so no component `ui` depends on
another. Then `feed:ui` knows every source's fields and grows by one card renderer per
source. That is the oversized module the component split exists to prevent.

### One convention plugin per layer, in an included build

`build-logic/` is added with `includeBuild`. It publishes one plugin per layer type:
`todayfeed.api`, `todayfeed.domain`, `todayfeed.data` and `todayfeed.ui`, plus
`todayfeed.application` and `todayfeed.core`. Each plugin holds its layer's settings *and*
its dependency rules. `todayfeed.ui` adds the Compose and Hilt setup plus
`:core:designsystem`. `todayfeed.api` sets up a plain JVM library and does not apply the
Android plugin at all, which is what makes "no Android in `api`" a build fact rather than a
promise.

A new component is then four build files of about two lines each, and it gets the
architecture for free. That is what makes twenty-one modules affordable.

`buildSrc` was the alternative. I rejected it because any change inside `buildSrc` makes
Gradle rebuild every module's build script. With twenty-one modules, a one-line build tweak
becomes a full rebuild.

Module dependencies are written as strings, like `project(":core:designsystem")`, instead of
typed project accessors. The typed version reads better at this module count, but it is
still a preview feature and the generated names for deeply nested paths are not obvious.
Strings cost nothing here and remove one moving part from the part of the build most likely
to waste time.

### Hilt, not the plain Dagger 2 that the reference architecture uses

The codebase this layout is modelled on uses Dagger 2 directly, with a hand-written
`@Component` graph and a factory for each component. That is right for that codebase. An
automotive app that spans several processes needs direct control over when a component graph
is created and destroyed, and Hilt's fixed Android scopes do not give that.

None of that applies here. This is a single-process phone app. Its injection points are
exactly the ones Hilt was made for: an `Application`, one `Activity`, and one ViewModel per
screen. Copying Dagger to match the reference would add a component graph, a factory and a
scope annotation per component, in exchange for control this app never uses.

This is worth writing down because it is a likely interview question. The reason to drop
down to plain Dagger is a lifetime Hilt cannot express, not a high module count.

### Hand-written fakes, not a mocking library

`:core:testing` holds shared test doubles: a `FakeClock`, fake versions of the `api`
interfaces, and coroutine test rules.

Slice 2's tests check behaviour over time. For example: an entry older than its time limit
triggers a refresh, and the old value is still served while the refresh runs. With a fake
clock and a fake source that counts calls, that test reads as a list of events. Written as
call expectations on a mock, it becomes a claim about the order of internal calls. That
tests the implementation instead of the behaviour, and it breaks whenever the implementation
is tidied up.

The `api` modules make fakes cheap. Every interface a test needs to fake is small and
framework-free, so a fake is a few lines and can be shared.

*Option rejected:* MockK, which the reference codebase uses and which is the usual Kotlin
choice. It earns its place against large old interfaces and final classes. Against small
interfaces we designed ourselves, fakes read better and do not need updating every time a
signature changes.

### Core library desugaring, decided here so slice 2 benefits

Turn on `isCoreLibraryDesugaringEnabled` with `desugar_jdk_libs`, once, in the Android
convention plugins. This gives us `java.time` — `Instant`, `Duration` and `Clock` — on API
24.

The gain shows up in `:core:freshness`. Code written against an injected `Clock` makes "this
entry is forty minutes old" a one-line setup in a test, instead of arithmetic on millisecond
values. The cost is about 100–200 KB of APK size and one dependency.

*Other options.* Use `Long` millisecond values everywhere: no build cost, but the freshness
tests get harder to read, and readable freshness tests are close to the centre of what this
assignment marks. Use `kotlinx-datetime`: a nicer API and ready for multiplatform, but its
JVM version *also* needs desugaring on API 24. It adds a dependency without removing the one
it was meant to replace.

### Type-safe navigation, with the graph owned by `:app`

One `NavHost` in `:app`, using `@Serializable` route types instead of string routes with
`{argument}` placeholders. `ArticleDetail(val articleId: String)` is checked at build time.
`"detail/{id}"` is checked at run time, and a typo in it crashes the app.

Components hand out screens plus callbacks, such as `onArticleClick(id)`. They never hand
out a destination. That keeps "no component depends on another component's `ui`" true for
navigation as well: `:app` is the only module that knows a card tap leads to detail.
`feed:ui`'s dependency on other `ui` modules is for drawing, not for routing.

Each tab keeps its own state, as the spec requires. That comes from saving and restoring the
back stack per destination, not from keeping two `NavHost`s alive at once. Two hosts is the
other common approach, and I rejected it because it makes "open detail from either tab"
unclear about which host owns the detail entry.

### Four content states, defined once, in `:core:designsystem`

A sealed interface with `Loading`, `Empty`, `Error`, `Offline` and `Content<T>`, plus one
composable for each state that is not content.

The interesting one is `Offline`, and it is on purpose **not** a kind of `Error`. Being
offline with cached content is a normal, non-failing state in an offline-first app. The user
sees real content plus an honest note that it may be old. That is exactly what slice 2
needs. If `Offline` were a kind of `Error`, the UI would have to choose between showing the
cached content and telling the truth about it.

One thing is *not* decided here: how the data layer reports "this is stale" upward. That
belongs with the cache that produces it, in slice 2.

*Option rejected:* use `kotlin.Result` plus a nullable data field on each screen. Less code,
but then every screen works out "loading, with old content" from a pair of nullable values.
That is exactly the logic that should live in one place.

### Brand colours instead of dynamic colour, and no setup mechanism

Hand-written light and dark Material 3 colour schemes based on the green from the reference
screens, with Material You dynamic colour turned off. This is a branded content product. A
feed that recolours itself to match the user's wallpaper is not what a LINE TODAY style
surface does. Both schemes are written at the same time, so dark mode is a real design and
not an inverted afterthought.

All four data sources are keyless, so the project has **no** `local.properties` reader, no
secret injected into `BuildConfig`, and no environment variable fallback. `assembleDebug` on
a fresh copy cannot fail because of setup, because there is no setup. CI proves that again on
every push. The rejected option was an optional-key path that hides the carousel when the key
is missing. That is what a real product would do for a source it cannot replace. Here it
bought a build-config mechanism, a second code path and a paragraph of README warnings, all
to show the same rectangle of poster art.

### detekt and ktlint, in the build and in CI

Lines up to 140 characters, 4-space indent, no star imports, at most 3 `return` statements
per function, at most 15 functions per class. `MagicNumber` applies to production code and is
relaxed in tests and DI modules. `.editorconfig` holds the formatting rules so the IDE and CI
agree.

The point is not the exact numbers. The point is that mechanical rules should be checked by a
tool and should never appear in a review comment. That leaves review for the things a tool
cannot see.

### The placeholder ViewModels do real work

Each screen ships one `@HiltViewModel` that holds nothing but a fixed state. They exist so
this change proves that the annotation processing, the `@AndroidEntryPoint` activity and the
module wiring all work together. A skeleton whose DI graph compiles only because nothing asks
it for anything is not worth having.

### CI

One workflow, on push and on pull request: JDK 17, Gradle with caching, then
`./gradlew assembleDebug detekt ktlintCheck testDebugUnitTest --stacktrace`. No emulator, no
signing, no secrets.

## Risks / Trade-offs

- **The convention plugins could eat a lot of time.** Reading the version catalog from an
  included build and wiring the Compose compiler are the usual sticking points. → Hard
  limit: if `build-logic` resists, fall back to plain per-module setup, write it down in
  `DECISIONS.md`, and move on. This slice must not spend the freshness policy's time.
- **Fifteen modules costs something on every later slice**, in wiring and in navigation
  indirection. → Accepted on purpose. Convention plugins make the twenty-second module
  nearly free, and the dependency guarantees are the reason for doing it.
- **Four separate Room databases may look like a mistake** rather than a choice. → The
  reasoning is above and goes into `DECISIONS.md`. A reviewer who disagrees should at least
  see that the cycle and the alternatives were understood.
- **The pinned versions may not be current.** → They are checked against the real
  repositories during implementation, not written from memory, and they live in one file so
  a fix is one edit.
- **Four free third-party APIs, none of which promise uptime.** A source that is down while
  the reviewer builds looks like a bug in my code. → That is what the offline and error
  states are for. Slice 2's cache turns a dead source into old content instead of a blank
  screen. All four were checked and responding before I committed to them.
- **The film API is the weakest link.** It is a community deployment on Vercel rather than
  an official API, and it sends `cache-control: no-cache`. → Its data is static and only 22
  items, so once cached it never needs to be fetched again. The freshness policy already
  gives it a very long time-to-live, which means a single successful fetch is enough for the
  rest of the session. If it turns out to be down often during slice 4, the fallback is to
  ship the 22 records as a bundled asset and treat the network as an optional refresh.
- **A walking skeleton can look like progress.** Fifteen wired modules showing
  placeholder text has no user value. → It is one timeboxed slice, and the real deliverable
  is the module layout, the desugaring decision and the layer rules.

## Open Questions

- The exact versions to pin for AGP, Kotlin, the Compose BOM, `compileSdk` and `targetSdk`.
  Left to implementation, where they can be checked against the repositories instead of
  recalled. Nothing above depends on which stable versions they turn out to be.
- Whether `serviceCard` should get a `domain` module once its card has real behaviour. It has
  none today. If slice 4 gives it some, adding the module is one build file, and the layer
  plugins make that a local change.
