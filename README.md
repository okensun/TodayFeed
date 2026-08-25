# TodayFeed

A content feed app in the style of LINE TODAY. One scrollable list mixes article cards
with a weather hero card. Open an article, save it, and read what you saved with no
network.

## Run it

```bash
./gradlew assembleDebug
```

That is the whole thing, on a machine with a JDK 17 and the Android SDK.

Gradle needs to know where the SDK is, through `ANDROID_HOME` or a `sdk.dir` line in
`local.properties`. Android Studio writes that file the first time it opens a project, and
every Android project needs it, so it is not something this one adds.

Beyond that there is nothing to configure. No API keys, no secrets, no local files to create,
because every data source is keyless. CI runs the same command on every push with no secrets
configured, so that stays true rather than being a claim.

To put it on a device: `./gradlew installDebug`.

## What is in it

| Screen | Shows |
|---|---|
| Reading | the feed: a weather hero card above a paginated list of articles |
| Detail | one article, readable offline once saved |
| Saved | the articles you kept |

Data comes from three free APIs that need no key: [Spaceflight
News](https://api.spaceflightnewsapi.net/v4/articles/) for articles and
[Open-Meteo](https://api.open-meteo.com/v1/forecast) for weather.

## How it is built

Component-based Clean Architecture. A component is one area of subject matter, and the four
layers `api`, `domain`, `data` and `ui` are the shape it takes. A layer is created only when
there is something to put in it, which is why not every component below has all four. Two rules
carry the design, and the build enforces both:

1. Only `:app` may depend on a `data` module, so a ViewModel cannot reach Retrofit or a
   DAO even by accident.
2. A component sees another component only through its `api` module.

```
:app                              navigation, bottom bar, Hilt aggregation
:core:designsystem                theme (light and dark), the four content states
:core:network                     OkHttp, Retrofit, serialization
:core:database                    shared Room settings
:core:freshness                   the per-source freshness policy
:core:testing                     FakeClock and shared fakes
:components:articles:{api,data,ui}
:components:weather:{api,data,ui}
:components:feed:{domain,ui}
```

Compose with Material 3, MVVM with `StateFlow`, Hilt, Room, Retrofit, and Paging 3 reading
from Room. `AGENTS.md` explains the layout in full. `DECISIONS.md` says why each of
those was chosen and what was turned down.

## Freshness

The screen always shows what is stored on the device. The network only tops that store up. A
refresh that fails therefore cannot empty the screen, and being offline needs no separate path
through the code.

Before asking a source for anything, the app looks at what it already holds. That decision is a
plain function in `:core:freshness`, and it has five answers:

| Stored | Connection | What happens |
|---|---|---|
| nothing | none | say so, and offer a retry |
| nothing | any | fetch |
| something | none | show it, marked as possibly out of date |
| something, inside the allowance | any | show it and ask for nothing |
| something, past the allowance | any | show it, then fetch |

### The allowance, and where the number comes from

Spaceflight News states its own: `cache-control: max-age=600`, which is ten minutes. Measured on
2026-08-24. That figure wins, because a number the source states is a fact while ours is a
judgement. Our own figure is used only when a source states none. Spaceflight News always states one, so
ours never applies there. Open-Meteo states none, so ours is what holds for the weather.

Open-Meteo states nothing, so our own figure is the one that counts there: fifteen minutes,
which is how often the source says it re-reads. It reports that as `interval` in its own answer.
The weather is held in memory rather than in a database, so a cold start with no network shows
the feed without the card rather than yesterday's weather as if it were now.

### What the reader keeps is not what the policy decides

Saving is the one decision the reader makes rather than the app. A saved article stays until they
unsave it: no refresh, no expiry and no allowance touches it, and it opens with no network. The
policy above decides when to ask the source for more; it has no say over what was kept.

### When it calls the network

It does:

- on the first open, when nothing is stored
- on a later open, when what is stored is older than the allowance
- when the reader pulls the list down, whatever the allowance says. Asking anyway is what a pull
  is for
- when the reader scrolls past the end of what is stored, for the next page only
- when the network comes back after being away

It does not:

- on a later open inside the allowance. Verified on an emulator: a relaunch within ten minutes
  makes **zero** requests and the articles stay on screen
- when there is no connection. Nothing is attempted, so nothing arrives as an error either
- when the reader scrolls back up through pages already held
- to renew the allowance while scrolling. Reading older articles says nothing about whether the
  top of the feed has changed, so fetching the next page leaves the allowance where it was

## Plan and sequencing

The brief hides its hardest part in one bullet: a freshness policy for a feed that mixes sources
which update at different speeds and still works offline. Feed, detail and save are ordinary
work. So the order is risk first, not features first.

| # | Change | Why here |
|---|---|---|
| 1 | `bootstrap-project-skeleton` | Nothing can be run or reviewed until a fresh copy builds. Drawing the module lines now is cheaper than drawing them after code has grown across a line nobody drew. |
| 2 | `article-freshness-and-feed` | The heart of it. `:core:freshness` first with its tests, then one source wired through Room as the single source of truth. |
| 3 | `save-articles` | The last must-have. It needs the article already stored, so it comes after slice 2. |

Sixteen hours were planned across four evenings, one slice each and the documents in the last
block. Two things did not go to plan, and both are worth stating:

- **Slice 2 was estimated at four hours and took closer to eight.** The estimate was made against
  a horizontal plan; rewriting it as five vertical passes, each ending with the app working, is
  what kept the overrun from being visible to a reader. The pass order also carries its own cut
  list, so running out of time removes the last pass rather than leaving something half-built.
- **Two things thought finished were not.** The weather card drew a fixed value from the skeleton
  until it was wired to Open-Meteo, and the article pictures were fetched and stored but never
  drawn. Both were found by using the app rather than by a test, which is the point made at
  length in `AI_USAGE.md`.

### What was cut, and why

- **The `serviceCard` component** (promotional cards). The heterogeneous feed needs articles plus
  one more source; the weather card is that source, so cutting this leaves every must-have whole.
- **Search and filter.** A nice-to-have needing debounce handling and its own empty-result state.
  Not worth an hour and a half against the freshness policy.
- **Animations and transitions.** Polish. First out.
- **A metered connection changing behaviour.** The connection is read for real and the enum has a
  `Metered` case, but nothing yet treats it differently from unmetered. The policy is written to
  take it, so this is a value not yet used rather than a design that cannot.
- **Location for the weather.** Fixed coordinates instead. A permission dialog on first launch
  costs more than it buys against "clean checkout, one command".
- **The `movie` component** (Studio Ghibli). Designed and left for time after the documents.

## How I worked with AI

Claude Code (Opus 5) wrote most of the code and most of the words. The process around it is the
part worth describing, because it is what makes the output reviewable:

- **OpenSpec** for every change. A proposal, a delta spec of observable behaviour, a design
  document and a task list, all committed under `openspec/changes/`. The plan is reviewable next
  to the code, and its revisions are in the history.
- **A skill library** for brainstorming, plan writing, spikes and code review.
- **Review by a subagent** that is given the diff and the requirements but **not** the session
  history, so it cannot inherit the author's blind spots. Its findings are posted as inline
  comments on the exact line. Every pull request after the first went through this.

My role was to set the constraints, price the claims and decide. Almost every significant
correction in this project came from a question rather than from spotting a bug: "why do some
components have no `domain` module", "is CI running the view tests", "why is the star not
filled". `AI_USAGE.md` is the short version of what that turned up.

## Known limitations

Written as they are found.

- **A refresh walks back five pages at most.** When articles have arrived since the reader was
  last here, a refresh keeps asking for the next page until one holds an article already stored,
  so no gap is left in the middle. It gives up after five pages, which is a hundred articles. A
  reader who has been away longer than that sees the newest hundred, then a gap, then what they
  had before. Scrolling down fills the gap in from where the walk stopped.
- **The spinner for the next page is rarely seen.** Paging asks for it five articles before the
  end, so it usually arrives before the reader gets there. It shows on a slow network, and after
  a failed page is retried.
- **Retry does nothing yet on the saved list.** Its Try again button is wired to a view model
  method with an empty body, because saving arrives in the next slice. The feed's retry and pull
  to refresh are real. The detail screen has nothing to reload, so its button says Go back and
  leaves the screen.
- **The weather is always Taipei.** The coordinates are fixed, so the app asks for no location
  permission. Reading a coarse location would mean a permission dialog on first launch, a path
  for when it is refused, and either reverse geocoding or a card that cannot name the place. None
  of that is what the brief asks for, and "clean checkout, one command" is worth more here.
- **A metered connection is read but never tested on one.** `NET_CAPABILITY_NOT_METERED` decides
  it, and wifi and aeroplane mode were both checked on a Pixel 6. The phone has no SIM, so
  mobile data reporting itself as metered is unverified.

### Checked by hand rather than by a test

There is no emulator in CI, so these were driven over `adb` on an emulator and the result
recorded. See `DECISIONS.md` for why.

| Behaviour | What was measured |
|---|---|
| Each tab keeps its own scroll position | Scrolled the feed, opened Saved, returned. The two screenshots are identical apart from the clock. |
| Tapping the tab already open adds nothing to the back stack | After tapping Reading while on Reading, the system back button left the app for the launcher. |
| The theme follows the system setting without a restart | Switching to dark redrew the app in dark, and the process id was unchanged before and after. |
| Detail opens for the article that was tapped, and returns | The detail screen showed the tapped card's title and no bottom bar. The system back gesture returned to the feed with its scroll position intact. |
| A fresh clone builds with one command | Cloned into an empty directory with no `local.properties`, ran `./gradlew assembleDebug`, got an APK. |

- **One scenario is still not verified: whether the screen keeps its state across a theme
  change.** On this emulator `adb shell cmd uimode night` swaps the task and the process, which
  loses the state for reasons that have nothing to do with the app, and writing the setting
  directly has no effect. The same check passes for an equivalent configuration change: after a
  font scale change the process, the task and the scroll position are all unchanged. It still
  needs a manual pass through Settings.
