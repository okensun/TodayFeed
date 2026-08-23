# Decisions

A running log. I add to it when I make a decision, not at the end of the week, so it says
what actually happened rather than what I remember.

Each entry has the same three parts: what I picked, what I considered instead, and the
trade-off I accepted.

The reasoning for the **freshness policy** is not here. It lives in the README, because the
brief asks for it there.

Status of each entry is either **decided** or **open**. Open entries name what is still
missing.

---

## The five the brief asks about

### State management — decided

**Picked.** MVVM with one-way data flow. Each screen has a ViewModel that exposes a single
`StateFlow` of a `ContentState`. `ContentState` is a sealed interface in `:core:designsystem`
with five cases: `Loading`, `Empty`, `Error`, `Offline` and `Content<T>`.

**Considered instead.** `kotlin.Result` plus a nullable data field on each screen. Also
Molecule, and a full MVI setup with reducers and a single event stream.

**Trade-off.** The sealed interface costs one shared type and forces every screen to name its
state explicitly. In return, "loading, but I already have old content" exists in one place
instead of being worked out from a pair of nullable values on every screen. MVI was rejected
as more machinery than four screens can justify; the reducers would carry no rules.

One detail worth naming: `Offline` is **not** a kind of `Error`. In an offline-first app,
being offline with cached content is normal and is not a failure. If `Offline` were a kind of
`Error`, every screen would have to choose between showing the cached content and telling the
user it might be old. It can do both.

### Dependency injection — decided

**Picked.** Hilt.

**Considered instead.** Plain Dagger 2 with a hand-written `@Component` graph per component,
which is what the production codebase this architecture is modelled on does. Also Koin.

**Trade-off.** Hilt fixes its scopes to the Android lifecycle. That is a real limit, and it is
exactly why the reference codebase does not use it: an automotive app spanning several
processes needs to control when a graph is built and destroyed. This app is one process, and
its injection points are the ones Hilt was built for — an `Application`, one `Activity`, one
ViewModel per screen. Copying Dagger would have added a graph, a factory and a scope
annotation per component in exchange for control this app never uses. Koin was rejected
because I would rather find a wiring mistake at build time than at run time.

The rule I would apply next time: reach for plain Dagger when you need a lifetime Hilt cannot
express, not when you have a lot of modules.

### Persistence — decided

**Picked.** Room, with **one database per component**. `:core:database` holds only shared Room
settings and the `Instant` type converter. It holds no tables.

**Considered instead.** One shared database holding every table in `:core:database`, which is
what Now in Android does. Also declaring the `@Database` class in `:app`.

**Trade-off.** The forcing function was a dependency cycle: a shared `@Database` class has to
name every table, so `:core:database` would have to depend on each component's `data` module,
and those already depend on it. Of the three ways out, one database per component also removes
shared migration risk — changing how articles are stored cannot break the weather cache — and
lets each component's storage be tested on its own with an in-memory database. The cost is four
small database files and no way to write a query across components. No such query exists: the
feed is assembled in memory from four independent sources.

### Compose or Views — decided

**Picked.** Jetpack Compose with Material 3.

**Considered instead.** XML views with Fragments and a `RecyclerView` using multiple view
types.

**Trade-off.** The feed mixes card shapes, and `RecyclerView` handles that with view-type
integers, separate `ViewHolder` classes and a delegate per type. In Compose it is a `when` over
a sealed type inside `LazyColumn`. Compose also makes the four content states cheap to render
consistently. The cost is that Compose previews are not a substitute for real device testing,
and that some of the team knowledge around `RecyclerView` performance does not transfer
directly.

### Concurrency — decided

**Picked.** Coroutines and Flow. `StateFlow` for state that always has a value. Dispatchers are
injected, never referenced directly, so tests can replace them. Flows are collected with
`repeatOnLifecycle`. Scopes are cancelled, never individual child jobs.

**Considered instead.** RxJava, and `LiveData` for the UI layer.

**Trade-off.** Injected dispatchers add one constructor parameter to most classes. In return,
every unit test runs on a test dispatcher with no real delays, which is what makes the freshness
tests fast and repeatable. `LiveData` was rejected because it would mean two state types in one
app for no gain.

---

## Architecture

### Component-based Clean Architecture, 21 modules — decided

**Picked.** Each area of subject matter is a *component* that owns its own `api`, `domain`,
`data` and `ui` layers. Two rules carry it: only `:app` may depend on a `data` module, and a
component may see another component only through its `api`.

**Considered instead.** One `:app` module with `data`, `domain` and `ui` packages. Also one
global three-layer split (`:domain`, `:data`, `:ui`) shared by all features.

**Trade-off.** 21 modules is more structure than a four-screen app needs, and the single-module
version would have been faster. I chose the modules because they turn two common review comments
into build errors: "this ViewModel talks to Room directly" and "this feed code reaches into the
weather cache" are both unwritable here. The global three-layer split is the more interesting
rejection — it keeps the layers but loses the boundary, because every feature shares one
`:domain`. Layers alone do not give the guarantee. Component ownership does.

The cost is paid by convention plugins in a `build-logic` included build, so each module's build
file stays about two lines and a new component inherits the rules for free.

### A `domain` module only when logic has no model to own it — decided

**Picked.** Only `articles` and `feed` get a `domain` module. The test: a `domain` module exists
when logic coordinates more than one repository or source, so no single model can own it.
`articles` has paging cursors, save and unsave against the cache, and freshness-driven refresh
decisions. `feed` combines four sources and decides what to do when one fails.

**Considered instead.** Give all five components the same four layers.

**Trade-off.** Uniformity is easier to learn, and an unused layer only costs a build file. But
`weather`, `serviceCard` and `movie` would each get a module holding one pass-through class, and
a pass-through class attracts logic that belongs elsewhere. Their logic is real — discount
arithmetic, sorting by air date, mapping a weather code — but it fits on their models in `api`,
which is plain Kotlin and may carry its own arithmetic.

Worth recording: my first draft of this justified it as "these components have no business
logic". That was wrong, and the corrected rule above is the one I can actually apply.

### `:components:feed:ui` may depend on other components' `ui` — decided

**Picked.** One exception to the component isolation rule. `:components:feed:ui` depends on the
`ui` modules of `articles`, `weather`, `serviceCard` and `movie`, because drawing their cards in
one list is its whole job. Card composables take `api` models directly.

**Considered instead.** Put every card composable in `feed:ui`, so no component `ui` depends on
another. Also give each component a UI model and a mapper.

**Trade-off.** The first option makes `feed:ui` know every source's fields and grow by one card
renderer per source — the oversized module the component split exists to prevent. The second
adds a type and a mapper per card type to buy protection the `api` boundary already gives.
Making `feed` a component rather than a screen inside `:app` keeps the cross-component knowledge
in exactly one module whose name says so.

---

## Data sources

### No API keys anywhere — decided

**Picked.** All four sources are keyless: Spaceflight News, Open-Meteo, DummyJSON and the Studio
Ghibli film API. The project has no `local.properties` reader, no secret in `BuildConfig` and no
environment variable fallback.

**Considered instead.** TMDB for the film carousel, with an optional-key path that hides the
carousel when the key is missing.

**Trade-off.** The optional-key path is what a real product would build for a source it cannot
replace. Here it bought a build-config mechanism, a second code path and a paragraph of README
warnings, all to show the same rectangle of poster art. Removing it means `./gradlew
assembleDebug` on a fresh copy cannot fail for a setup reason, because there is no setup, and CI
proves that again on every push.

### Studio Ghibli for the film carousel, not TVMaze — decided

**Picked.** `https://ghibliapi.vercel.app/films`. 22 films, keyless, with a poster and a wide
banner image.

**Considered instead.** TVMaze, whose schedule endpoint changes daily and would have added a
fourth update speed to the freshness policy.

**Trade-off.** TVMaze serves TV shows, not films, so a component named `movie` holding TV
schedule data would be a name that lies about its contents. I took honest naming over a richer
freshness story. The static film list turns out to be useful anyway: its correct time-to-live is
"effectively forever", which is only expressible if the policy holds a value per source rather
than one number for the whole app.

The known weakness: the host is a community deployment on Vercel, not an official API, so it is
the least reliable of the four. Because the data is static and small, one successful fetch is
enough. If it proves unreliable, the fallback is to ship the 22 records as a bundled asset and
treat the network as an optional refresh.

### Component names come from the brief's own words — decided

**Picked.** `articles`, `weather`, `serviceCard`, `movie`, `feed`.

**Considered instead.** `shopping`, `offers` and `promotions` for the DummyJSON cards.
`tvschedule` for the carousel.

**Trade-off.** `shopping` describes the data source rather than what the card does in the feed.
`serviceCard` is the brief's own term, so a reviewer can map the repository to the requirements
without a translation step. The general rule I am applying: a module name must match what is
really inside it, and should use the vocabulary the reader already has.

---

## Build and tooling

### Hand-written pagination, not Paging 3 — decided

**Picked.** Pagination written by hand: a repository that holds the cursor and exposes a
`StateFlow` of the loaded page state.

**Considered instead.** Paging 3 with a `RemoteMediator`, which is the official offline-first
answer.

**Trade-off.** `RemoteMediator` gives the framework control over when a load happens. The
centre of this assignment is a per-source freshness policy with stale-while-revalidate, and I
want that decision in my own code where I can unit test it against a fake clock. The cost is
that I write the paging state machine myself, including the edge cases Paging 3 already handles,
and I lose its built-in placeholders and retry plumbing.

### AGP 9.2.1, not the newest 9.3.1 — decided

**Picked.** Android Gradle Plugin 9.2.1, with Gradle 9.7.1 and Kotlin 2.3.21.

**Considered instead.** AGP 9.3.1, which was the newest stable release and which I started
with. Also AGP 8.13.2, the previous major.

**Trade-off.** 9.3.1 built fine from the command line, then Android Studio 2025.3 refused to
open the project: it supports up to 9.2.1. My first thought was that my own tools were
behind, but the reviewer's Studio is likely the same stable release. A project that builds
on the command line and cannot be opened in the IDE is worse than one major-minor version
behind, because opening it is the first thing anyone does. So the version ceiling here is
not "newest released", it is "newest that a stable Android Studio can open".

I kept AGP 9 rather than dropping to 8.13.2, because 9.x is where the built-in Kotlin
support and the new DSL live, and the migration work was already done.

Worth recording as a general rule: for a deliverable someone else opens, check the IDE's
supported range, not only the artifact repository.

### Core library desugaring, so `java.time` works on API 24 — decided

**Picked.** `isCoreLibraryDesugaringEnabled` with `desugar_jdk_libs`, set once in the Android
convention plugins.

**Considered instead.** `Long` millisecond values throughout. Also `kotlinx-datetime`.

**Trade-off.** About 100–200 KB of APK size and one dependency, in exchange for `Instant`,
`Duration` and an injectable `Clock` on `minSdk 24`. That makes "this entry is forty minutes
old" a one-line setup in a test instead of arithmetic on millisecond values, and readable
freshness tests are close to what this assignment marks. `kotlinx-datetime` was rejected because
its JVM implementation also needs desugaring on API 24, so it adds a dependency without removing
the one it was meant to replace.

### Hand-written fakes, not a mocking library — decided

**Picked.** Test doubles written by hand and shared from `:core:testing`, including a
`FakeClock`.

**Considered instead.** MockK, which is the usual Kotlin choice and is what the reference
codebase uses.

**Trade-off.** Mocks would save writing the fakes. But the freshness tests check behaviour over
time — an entry past its time limit triggers a refresh, and the old value is still served while
the refresh runs. With a fake clock and a fake source that counts calls, that reads as a list of
events. As call expectations on a mock it becomes a claim about the order of internal calls,
which tests the implementation rather than the behaviour and breaks when the implementation is
tidied up. The `api` modules make fakes cheap, because every interface a test needs is small and
framework-free.

### detekt and ktlint in CI — decided

**Picked.** Both run in the build and in CI. 140-character lines, 4-space indent, no star
imports, at most 3 returns per function, at most 15 functions per class.

**Considered instead.** Nothing, and relying on review.

**Trade-off.** Some setup time. In return, mechanical rules never appear in a review comment,
which leaves review for what a tool cannot see.

---

## Process

### OpenSpec for spec-driven changes — decided

**Picked.** Each slice of work is an OpenSpec change with four artifacts: a proposal, a delta
spec of observable behaviour, a design document, and a task list. They are committed, so the
plan is reviewable next to the code.

**Considered instead.** Working straight from the brief, with notes in a scratch file.

**Trade-off.** The artifacts cost real writing time before any code exists. They pay for
themselves in three ways here: the delta spec turns the brief's requirements into scenarios that
can be checked one by one, the task list gives natural commit boundaries so the git history is
readable, and the design document is where a decision like "one database per component" gets
argued before it is built rather than defended afterwards.

### Plain English in every document — decided

**Picked.** All documents, comments and commit messages are written in plain, general English
that a reader below high-school English level can follow. Short sentences, one idea each, common
words.

**Considered instead.** Denser technical prose, which is what my first drafts used.

**Trade-off.** Plain English takes more passes to write and can feel less precise. But the
reviewers are not native English speakers and communication is a graded item, so simple wording
raises the score rather than lowering it. A decision nobody can read has not been communicated.

---

## Freshness

The user-facing explanation of the policy lives in the README, because the brief asks for it
there. What follows is only the design decisions behind it.

### Two dimensions, both measured rather than guessed — decided

**Picked.** The policy answers two separate questions. *When is it worth asking?* comes from a
per-source time-to-live, chosen from what the card means in a real product. *What does asking
cost?* comes from whether the source can be revalidated cheaply.

I checked all four sources instead of assuming. Spaceflight News sends
`Cache-Control: max-age=600`, so the server states its own freshness and that beats any number
I would invent. Open-Meteo sends no cache headers at all. DummyJSON and the Ghibli API both
answer `304 Not Modified` with zero bytes when sent `If-None-Match`.

**Considered instead.** A single time-to-live per source and nothing else, which is where my
first draft stopped.

**Trade-off.** The second dimension costs one more concept to explain. It earns it, because it
reverses an intuition that would otherwise have led me wrong. "Static data is cheap to refresh"
sounds obvious, but the two static sources are exactly the two that answer 304, so asking them
costs nothing. The expensive sources are the ones with no conditional request support, which
here means the weather. Without measuring, I would have given the cheap sources the long
time-to-live and the expensive one the short one.

### Refresh only at moments the user can see — decided

**Picked.** A refresh can start when the app comes to the foreground, when a tab is shown, on
pull to refresh, and when scrolling to the end for the next page. There is no WorkManager and
no background work of any kind.

**Considered instead.** Periodic background refresh, so the feed is already warm when opened.

**Trade-off.** The feed can be a few minutes stale at the moment the app opens, and the first
paint may show cached content while a refresh runs. In exchange, the app never spends the
user's mobile data while they are not looking, which is what the brief warns about. It also
removes battery, doze mode and scheduling from the design entirely.

### A metered connection lengthens every time-to-live — decided

**Picked.** Each source declares two times-to-live, one for an unmetered connection and one for
a metered one. Sources that cannot be revalidated cheaply are stretched further on a metered
connection. Connectivity arrives as an injected interface returning `Unmetered`, `Metered` or
`Offline`, so `:core:freshness` stays plain Kotlin with no Android dependency.

**Considered instead.** One time-to-live regardless of connection.

**Trade-off.** Two numbers per source to justify instead of one, and a second fake in the
tests. It is worth it because the brief's words are "without wasting the user's mobile data",
and this is the difference between a policy that has a data-cost dimension and a README that
merely claims one.

### Four decisions, not two — decided

**Picked.** The policy returns one of `ServeCache`, `ServeCacheAndRevalidate`, `FetchBlocking`
or `ServeStaleOffline`. `ServeCacheAndRevalidate` is stale-while-revalidate: show what we have
at once, refresh behind it, replace when it arrives.

**Considered instead.** A boolean "should I refresh".

**Trade-off.** A boolean cannot express the difference between "nothing cached, so the user
must wait" and "something cached, so show it and refresh quietly". Those are different screens.
The four cases line up with the four content states in the UI, which is not a coincidence: the
UI state machine is a projection of the policy's decision.

### Saved articles never expire — decided

**Picked.** Freshness governs the feed. It does not govern the user's saved copies. A saved
article is never evicted and never goes stale.

**Considered instead.** One eviction policy for everything.

**Trade-off.** Saved articles can grow without bound, which needs a limit eventually. But
"saved items are readable offline after first load" is a must-have in the brief, and a cache
policy that can delete something the user explicitly kept would break it. The article body is
written at the moment the user saves it, not looked up later.

## Open

- **How the data layer reports staleness upward.** The UI already has an `Offline` state that
  can carry content. What the repository returns so the ViewModel knows the content is old is
  not decided; it belongs with the cache that produces it, in slice 2.
- **Whether `serviceCard` needs a `domain` module.** It has no coordinating logic today. If
  slice 4 gives it some, adding the module is one build file.
- **Exact pinned versions** for AGP, Kotlin, the Compose BOM, `compileSdk` and `targetSdk`. To
  be checked against the real repositories during implementation, not written from memory.
