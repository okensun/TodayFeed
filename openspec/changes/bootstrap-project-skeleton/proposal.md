## Why

Nobody can review, test or run this project until a fresh copy builds. The brief asks for
exactly that: the app must build from a clean checkout with one command. CI is also on the
nice-to-have list.

There is a second reason to do this first. The module layout is the one decision here that
gets more expensive every day we wait. On an empty repository, deciding which module may
depend on which is almost free. After feature code has grown across a line we never drew,
moving that line is slow and risky. "We will split it into modules later" usually means we
never do.

So this change builds a walking skeleton. Every module the app will ever need exists.
Every screen can be reached. All of it shows placeholder content. Later slices then fill
modules in instead of inventing them.

## What Changes

- The Gradle build becomes a Kotlin-DSL multi-module build. It has one version catalog
  (`gradle/libs.versions.toml`) and a set of **convention plugins** in a `build-logic`
  included build. Twenty-one modules then share one copy of the Android, Kotlin, Compose
  and Hilt setup instead of twenty-one copies that slowly drift apart.

- **Component-based Clean Architecture.** A *component* is one area of the app's subject
  matter, not one screen. Each component owns its own layers: `api`, `domain`, `data` and
  `ui`. The build enforces the layering, so it does not rely on people remembering it.
  Fifteen modules to start with:

  | Component | Layers | Data source |
  |---|---|---|
  | `articles` | `api`, `domain`, `data`, `ui` | Spaceflight News |
  | `weather` | `api`, `data`, `ui` | Open-Meteo |
  | `feed` | `domain`, `ui` | puts the others together |

  plus `:core:designsystem`, `:core:network`, `:core:database`, `:core:freshness`,
  `:core:testing`, and a thin `:app`. A `movie` component (`api`, `data`, `ui`, backed by
  the Studio Ghibli film API) is designed but built only if the schedule allows, after the
  submission documents are done. With the convention plugins in place, adding it is three
  build files of about two lines each.

- Two dependency rules give the layout its value, and the convention plugins state them
  once:
  1. Only `:app` may depend on a `data` module. A component's `api`, `domain` and `ui`
     never do, not even their own component's.
  2. A component may only see another component through its `api` module. The one
     exception is `:components:feed:ui`, which depends on the other components' `ui`
     modules because drawing their cards in one list is its whole job.

- `:core:freshness` is a new plain-Kotlin module with an injected `Clock`. It is empty in
  this change. Slice 2 puts the per-source freshness policy there. It is a separate module
  so that policy can be unit tested with no Android, no Room and no HTTP on the classpath.

- Every component keeps its own Room database. `:core:database` only holds shared Room
  settings and type converters.

- `minSdk 24`, Kotlin, Compose with Material 3, and core library desugaring so that
  `java.time` works on API 24.

- Hilt is wired from end to end: a `@HiltAndroidApp` application class, one
  `@AndroidEntryPoint` Compose activity, and one placeholder `@HiltViewModel` per screen.
  The placeholders make sure the graph really works instead of only compiling.

- `:core:designsystem` holds the Material 3 theme with **both** a light and a dark colour
  scheme. It also holds one shared set of four content states: loading, empty, error and
  offline. The brief asks for all four to be handled clearly, and one shared set stops each
  component from inventing its own later.

- Compose Navigation gives us the shell from the reference screens: a bottom bar with
  *Reading* and *Saved*, plus an article detail screen. The navigation graph lives in
  `:app`. Components hand out callbacks, not destinations.

- detekt and ktlint are added to the build and to CI. Lines up to 140 characters, 4-space
  indent, no star imports, at most 3 `return` statements per function, at most 15 functions
  per class.

- GitHub Actions builds the debug variant, runs the style checks and runs the unit tests on
  every push and pull request. No secrets are set up, so CI keeps proving that the
  single-command rule still holds.

- `AGENTS.md` is the full guide for AI agents working in this repository. `CLAUDE.md` is a
  short summary that points at it. There is also a `README.md` stub with the one-line run
  command and a pull request template.

Some things are left out on purpose: any HTTP call, any Room table, any real content, the
freshness policy itself, save and unsave, and search. Those are later slices in
`docs/ROADMAP.md`. Adding them here would make this change too large to review.

**The trade-off, stated plainly.** Twenty-one modules is more structure than a four-screen
app needs. The honest alternative was one `:app` module with `data`, `domain` and `ui`
packages inside it. That is faster to build and fine at this size. I chose the modules
anyway, because this is a senior-level assessment and the module layout is the cheapest way
to show how I stop a real codebase's dependencies from decaying. Convention plugins absorb
most of the cost, so each module's build file stays a few lines long.

There was a middle option too, and rejecting it is the more useful part. That option is one
global `:domain`, one `:data` and one `:ui` for the whole app. It looks like Clean
Architecture and costs a third as many modules. But every feature shares the same
`:domain`, so nothing stops the article code from reaching into the weather cache. Layers
alone do not give that guarantee. Component ownership does.

From the brief, this change covers the *multi-module structure*, *dark theme* and *CI*
nice-to-haves, plus the *builds from a clean checkout with a single command* rule. It
prepares the *all UI states handled explicitly* must-have but does not finish it, because a
placeholder screen cannot really show an error state.

## Capabilities

### New Capabilities
- `app-shell`: starting the app, moving between the Reading and Saved destinations, opening
  and leaving an article detail screen, and following the system light or dark theme
  setting.

### Modified Capabilities

None. This is the first change in the project.

## Impact

- **New files**: `settings.gradle.kts`, the root `build.gradle.kts`,
  `gradle/libs.versions.toml`, the `build-logic` convention plugins, the Gradle wrapper,
  fifteen module directories, `config/detekt/detekt.yml`, `.editorconfig`,
  `.github/workflows/ci.yml`, `.github/pull_request_template.md`, `AGENTS.md`,
  `CLAUDE.md`, `README.md`.
- **New dependencies**: Kotlin, AGP, the Compose BOM and Material 3, Compose Navigation,
  Hilt, `desugar_jdk_libs`, JUnit, detekt, ktlint. Retrofit and Room are not added yet.
  They arrive with the slice that uses them, so the dependency list always matches what the
  app really does.
- **Main risk**: the convention plugins. If `build-logic` fights back, the fallback is
  plain per-module setup plus a note in `DECISIONS.md`. It must not eat time that belongs
  to the freshness policy.
- **User-visible behaviour**: only the shell. The app starts, the tabs switch and keep
  their state, detail opens and closes, and the theme follows the system setting. No real
  content is shown.
