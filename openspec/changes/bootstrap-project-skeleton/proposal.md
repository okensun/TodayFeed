## Why

Nothing in this project can be reviewed, tested or demoed until a clean checkout
compiles and launches. The brief makes that literal: the app must build from a clean
checkout with a *single* command, and CI is one of the named nice-to-haves. Standing up
the module graph, theme and navigation shell first also settles the structural decisions
while they are still cheap — "we'll modularise later" reliably becomes "we never did",
and retro-fitting a module boundary once feature code has grown across it costs far more
than drawing it now.

This change deliberately builds a walking skeleton: every screen the app will ever have
exists and is reachable, but shows placeholder content. That gives the later slices a
place to land and gives us a runnable app to check the theme and navigation against from
day one.

## What Changes

- Gradle build converted to a Kotlin-DSL multi-module setup with a single version
  catalog (`gradle/libs.versions.toml`) and **convention plugins** in `build-logic/`, so
  the ten modules share one definition of the Android/Kotlin/Compose/Hilt setup instead
  of ten drifting copies.
- New module graph: `:app`, `:core:model`, `:core:network`, `:core:database`,
  `:core:data`, `:core:designsystem`, `:core:testing`, `:feature:feed`,
  `:feature:detail`, `:feature:saved`. Created empty-but-wired in this change; the
  `:core:network` and `:core:database` modules carry no networking or persistence code
  yet.
- `minSdk 24`, Kotlin, Jetpack Compose with Material 3.
- Hilt wired end to end: `@HiltAndroidApp` application class, a single Compose
  `@AndroidEntryPoint` activity, and one placeholder `@HiltViewModel` per feature so the
  graph is proven to build rather than merely declared.
- `:core:designsystem` provides the Material 3 theme with **both** a light and a dark
  colour scheme plus typography and spacing tokens, and the app follows the system
  setting.
- Compose Navigation with the shell from the reference screens: a bottom bar with
  *Reading* and *Saved* destinations, plus an article-detail destination that takes an
  article id and returns.
- A reusable set of UI-state composables in `:core:designsystem` — loading, empty, error
  and offline — because the brief requires all four to be handled *explicitly* and a
  shared vocabulary now stops each feature inventing its own later.
- GitHub Actions workflow: assemble the debug variant and run unit tests on every push
  and pull request.
- `README.md` stub carrying the one-line run command and a short overview.

Out of scope, by design: any HTTP call, any Room entity or DAO, any real feed content,
the freshness policy, save/unsave, search. Those are later slices in
`docs/ROADMAP.md`; mixing them in here would make this change impossible to review.

**Trade-off, stated plainly.** Ten modules and a `build-logic` project are more
ceremony than a four-screen app needs, and the honest alternative was a single `:app`
module with `data`/`domain`/`ui` packages — faster to stand up, and defensible for an
app this size. Rejected because the brief names multi-module structure as a nice-to-have
and this is a senior-level assessment: the module graph is the cheapest available
evidence of how I'd keep a real codebase's dependencies from rotting. The cost is
absorbed by convention plugins, which keep the per-module build files to a few lines.
The related rejected alternative is per-module `dependencies` blocks copy-pasted across
modules; rejected because the drift it causes is exactly the problem modularisation is
supposed to solve.

Satisfies from the brief: the *multi-module structure*, *dark theme* and *CI (build +
test on push)* nice-to-haves, and the *builds from a clean checkout with a single
command* ground rule. Lays the groundwork for the *all UI states handled explicitly*
must-have without claiming it — a placeholder screen cannot demonstrate an error state.

## Capabilities

### New Capabilities
- `app-shell`: launching the app, top-level navigation between the Reading and Saved
  destinations, navigating into and back out of an article detail destination, and
  honouring the system light/dark theme setting.

### Modified Capabilities

None — this is the first change in the project.

## Impact

- **New**: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`,
  `build-logic/` convention plugins, Gradle wrapper, ten module directories,
  `.github/workflows/ci.yml`, `README.md`.
- **Dependencies added**: Kotlin, AGP, Compose BOM + Material 3, Compose Navigation,
  Hilt, JUnit. No networking or persistence libraries yet — they arrive with the slices
  that use them, so the dependency list always reflects what the app actually does.
- **Risk**: convention plugins are the one piece here that can bite. If `build-logic`
  fights us, the fallback is plain per-module configuration and a note in
  `DECISIONS.md`; it must not be allowed to consume time that belongs to the freshness
  policy.
- **No user-facing behaviour beyond the shell.** The app launches, tabs switch, detail
  opens and closes, and the theme follows the system. Nothing displays real content.
