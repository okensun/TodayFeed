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

### Repository interfaces live in `api`, not in `domain` — decided

**Picked.** Each component's `api` module holds the repository interface and the models it
speaks in. `data` implements the interface, `ui` and `domain` compile against it, and `:app`
binds one to the other.

**Considered instead.** Putting the repository interfaces in `domain`, which is what classic
Clean Architecture does: the inner circle declares what it needs, and the outer circle
implements it. Also a separate `dataApi` module per component, which is what the production
codebase this structure is modelled on does.

**Trade-off.** Interfaces in `domain` would mean `data` depends on `domain`. That reads
oddly, and worse, it does not work for every component here: `weather` has no `domain`
module, so its interface would have nowhere to live except `api`, and the two components
would be laid out differently for no reason a reader could guess. Putting the interface in
`api` is the same in every component whether it has a `domain` module or not.

The separate `dataApi` module is the more careful version, because then another component can
see our models without seeing our repository. I merged the two, which means
`:components:feed:domain` can call `ArticleRepository` directly. That is exactly what it
needs to do, so at this size the extra module would buy nothing. The looseness it allows is
that `feed:ui` could also call a repository directly, bypassing `feed:domain`. Nothing
structural prevents that; it is a review matter.

Worth naming because "why aren't the repository interfaces in the domain layer" is the first
question anyone who knows Clean Architecture will ask.

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

### Paging 3, after three wrong reasons for avoiding it — decided, reversing an earlier decision

**Picked.** Paging 3 with a `RemoteMediator`, `room-paging` for the `PagingSource`, and
`LazyPagingItems` in the screen. The freshness decision stays ours, in
`RemoteMediator.initialize()`.

**Considered instead.** Writing the paging by hand, which is what I decided first and wrote a plan
around.

**Trade-off.** The reasons I gave for hand-writing it were three, and all three were false.
`RemoteMediator` does not take the load decision away — `initialize()` exists precisely so an app
can consult its own cache first, and a `RemoteMediator` is an ordinary class a test can drive.
Refresh does not have to lose the reader's place — the refresh branch need not delete, and Paging
reloads around `PagingState.anchorPosition`. And `PagingData` can be driven by a JVM view test,
through `PagingData.from(items, sourceLoadStates = ...)`.

I checked all three with a throwaway spike rather than arguing: five tests covering `room-paging`
under KSP on Kotlin 2.3 with AGP 9, the policy in `initialize()`, a `PagingData` flow rendering in
a Robolectric view test, and the four content states coming out of a plain function over
`loadState` and `itemCount`. All passed, then the spike was deleted.

The pattern in those three mistakes is worth more than the conclusion. Each was of the form "the
library cannot do X", when the accurate statement was "the library does X differently, and the
difference has a specific shape". Looking for a reason to keep a decision already made produces
"cannot" rather than "how".

What the choice costs: `ContentState` for the feed is derived from load states rather than handed
down by a view model, so one type has two sources; the refresh branch fetches several pages inside
one `load()` call, which is legal but unusual; and interleaving is constrained, which is the next
entry. What it buys is not much time — about half an hour — but it removes the paging state
machine, which was the single part of the plan most likely to overrun.

### A promotional card between articles would use `insertSeparators` — not built

**Picked.** Nothing. The service cards from DummyJSON were cut for time. This entry records how
they would be added, because "we can add that later" is the kind of assumption that quietly turns
out to be expensive.

**How it would work.** `PagingData.insertSeparators` inserts an item between two adjacent items:

```kotlin
paging.map<Article, FeedItem> { FeedItem.ArticleRow(it) }
    .insertSeparators { before, after -> promoOrNull(before, after) }
```

The article type has to be mapped into a sealed `FeedItem` first, because the separator type must
be a supertype of the item type. The promotional content itself is collected separately and
combined in, so it keeps its own allowance and no network call happens inside the transform.

**The one real constraint.** The generator must be a pure function of the two adjacent items. It
is called per boundary over whatever window is loaded, and the same boundary can be visited more
than once as pages are re-collected, so a counter held outside it would be wrong. That makes
"insert on a day boundary" direct, because both items carry a published time, and "insert every
tenth article" need the article to carry a stable ordinal — which the schema deliberately does not
have, because ordering comes from the published time instead.

**Trade-off.** This is the one direction where the hand-written version would have been easier: a
plain `List<FeedItem>` composed in `feed:domain` can have anything inserted anywhere in one line.
Paging 3 costs that flexibility. It is a flexibility for a feature that was cut, so the exchange
is worth it here — but it is worth writing down rather than discovering later.

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

### View tests on the JVM, device tests by hand — decided, reversing an earlier cut

**Picked.** View tests run through Robolectric with the Compose test rule, inside the ordinary
`test` task, so CI needs no emulator. They answer one question: given a state, does the screen
draw the right thing and do its callbacks fire. Device behaviour is checked by hand over `adb`,
with the evidence recorded.

**Considered instead.** No UI tests at all, which is what I decided first. Also an emulator in
CI through `android-emulator-runner`.

**Trade-off.** The original reasoning was that Compose UI tests need an emulator and would cost
more than they prove. Half of that was wrong: Robolectric runs them on the JVM. What changed my
mind was evidence rather than principle. A code review found two bugs in how the screens map
`ContentState` to what the user sees, and both are exactly what a view test asserts. Those two
bugs are now regression tests.

An emulator in CI would add six to twelve minutes to a two and a half minute run, and it is
famously unreliable. The chosen option costs about twelve seconds of Robolectric startup per
module, three modules, so roughly thirty six seconds. What it would buy is repeatability for scenarios I have already checked by
hand once: tab back stacks, the system back gesture, a theme change. Against a Thursday
deadline, that budget is better spent on the freshness policy, which is what the brief actually
weighs. So device checks stay manual. The README lists each one and what was measured, including the
one that is still unverified.

The cost that remains: Robolectric ships one jar per Android level and lags the newest.
`compileSdk` is 37 because the Compose BOM requires it, and a library module takes its manifest
`targetSdk` from `compileSdk`, which Robolectric then refuses. The emulated level is pinned to
36 in one properties file rather than annotated on every test class.

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

### What "offline" means for a screen that only reads local storage — decided

**Picked.** The Saved screen handles `Offline` exactly as the feed does: cached content is
shown, and `Offline(null)` shows the offline state with a retry. Its repository is not
expected to emit `Offline` at all, because it reads local storage.

**Considered instead.** Reusing the empty state's visuals for `Offline(null)`, on the grounds
that a local-only screen with nothing stored means nothing was ever saved.

**Trade-off.** The rejected option reads well in isolation and is what I wrote first. It fails
two requirements at once: the four states have to be distinguishable from each other, and here
`Offline` and `Empty` would be identical; and offline has to offer a retry, which the empty
state does not. It also made the same state look different on two screens.

Handling a case the repository should never produce is defensive code, which is normally worth
avoiding. It earns its place here because the alternative is an `else`, and an `else` over a
sealed state type is how a wrong screen reaches production without a warning.

## Open

- **How the data layer reports staleness upward.** The UI already has an `Offline` state that
  can carry content. What the repository returns so the ViewModel knows the content is old is
  not decided; it belongs with the cache that produces it, in slice 2.
- **Whether `serviceCard` needs a `domain` module.** It has no coordinating logic today. If
  slice 4 gives it some, adding the module is one build file.
- **Exact pinned versions** for AGP, Kotlin, the Compose BOM, `compileSdk` and `targetSdk`. To
  be checked against the real repositories during implementation, not written from memory.
