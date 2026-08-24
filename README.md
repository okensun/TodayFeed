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
judgement. Our own figure, fifteen minutes, is used only when a source states none. This source
always states one, so the fallback has never yet been needed.

The weather card is still a fixed value held in memory, so none of this applies to it yet.

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

To be written in the final block, from `docs/ROADMAP.md`. It covers how the problem was
broken into slices, the order they were built and why, and what was cut.

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
