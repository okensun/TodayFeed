# TodayFeed

A content feed app in the style of LINE TODAY. One scrollable list mixes article cards
with a weather hero card. Open an article, save it, and read what you saved with no
network.

## Run it

```bash
./gradlew assembleDebug
```

That is the whole thing. You need a JDK 17 and the Android SDK, and nothing else. There
are no API keys to obtain and no configuration to write, because every data source is
keyless. CI runs the same command on every push, with no secrets configured, so this stays
true.

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

Component-based Clean Architecture across 15 modules. A component is one area of subject
matter, and it owns its own `api`, `domain`, `data` and `ui` layers. Two rules carry the
design, and the build enforces both:

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
:components:articles:{api,domain,data,ui}
:components:weather:{api,data,ui}
:components:feed:{domain,ui}
```

Compose with Material 3, MVVM with `StateFlow`, Hilt, Room, Retrofit, and pagination
written by hand. `AGENTS.md` explains the layout in full. `DECISIONS.md` says why each of
those was chosen and what was turned down.

## Freshness

To be written in the final block. It covers what "fresh" means here, the time-to-live for
each source and where those numbers come from, why refresh only happens at moments the
user can see, and how a metered connection changes the answer.

## Plan and sequencing

To be written in the final block, from `docs/ROADMAP.md`. It covers how the problem was
broken into slices, the order they were built and why, and what was cut.

## Known limitations

To be written in the final block.
