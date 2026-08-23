## Context

The skeleton is in place: fifteen modules, the four content states, the screens, and an
`ArticleRepository` interface that an in-memory implementation currently satisfies.
`:core:freshness` holds nothing but an injectable clock.

Slice 1's design left one question open on purpose: how the data layer tells the UI that what it
is holding may be old. That is answered here, because the answer belongs with the cache that
produces the staleness.

Four things were measured by calling the sources, and two of them changed this design after it
was first written.

| Source | States its own max age | Answers 304 | Response size |
|---|---|---|---|
| Spaceflight News | **yes, `max-age=600`** | no | 8.6 KB for ten articles, so about 17 KB a page |
| Open-Meteo | no | no | small |
| DummyJSON | no | yes, empty body | 15 KB |
| Ghibli films | no (`no-cache`) | yes, empty body | 32 KB |

An article thumbnail from this source is around 50 KB. **A page is 17 KB of text and up to a
megabyte of pictures.** Any argument about the reader's mobile data that talks about refresh
intervals is arguing about the rounding error.

The second measurement is about publishing rate. This source publishes roughly twenty to forty
articles a day. A reader who opens the app once a day is therefore more than a page behind every
time, which decides how refresh has to work.

See `DECISIONS.md` for the decisions behind the policy. This document says how it is built.

## Goals / Non-Goals

**Goals:**

- The policy is a pure function with no hidden inputs, and its result needs no further
  interpretation by the caller.
- Room is the only thing the UI reads. A failed fetch cannot empty the screen.
- Every scenario in the `article-feed` spec is reachable in a test with fakes, no network.
- The reader never waits on a blank screen while something is stored.
- The claims about network use are numbers from a counting fake, not prose.

**Non-Goals:**

- No cache eviction. The table grows; the next slice needs saved articles kept forever anyway.
- No conditional requests. This source sends no ETag, so there is nothing to send. What was
  measured about the other sources goes in the README as what a second source would change.
- No image transformations. Coil, one thumbnail and one wide picture. The source's image URLs
  point at arbitrary hosts with no size parameters, so the only levers are whether to download
  and whether to look ahead.
- No search, no save, no weather.

## Decisions

### The policy is one function, and its answer is final

```kotlin
// :core:freshness

/** What a byte costs right now: nothing, money, or it cannot be had. */
enum class Connection { Unmetered, Metered, Offline }

sealed interface Decision {
    /** Young enough that asking would change nothing. */
    data object ServeCache : Decision

    /** Past its allowance, and something is stored. Show that, refresh behind it. */
    data object ServeCacheThenFetch : Decision

    /** Nothing is stored. The reader has to wait. */
    data object Fetch : Decision

    /** No network, but something is stored. Show it and say it may be old. */
    data object ServeCacheStale : Decision

    /** No network and nothing stored. */
    data object NothingToServe : Decision
}

fun decide(
    cachedAt: Instant?,        // null when nothing is stored
    serverMaxAge: Duration?,   // from Cache-Control, when the source states one
    timeToLive: Duration,      // our figure, used when the source states none
    connection: Connection,
    now: Instant,
): Decision
```

Five cases, and none of them needs the caller to work anything out. An earlier draft had four,
with one case meaning either "show what you have" or "there is nothing to show" depending on
whether the cache happened to be empty — which left the caller re-deriving what the function was
supposed to have decided. The four also mixed two questions in one type: what to do, and how to
describe what is being shown.

That draft existed to make the four cases line up with the four content states, and the design
called the alignment "not a coincidence". It was a flourish, and shaping a type to preserve it
was the wrong trade. Five decisions map onto four states perfectly well; two of them just
produce the same state.

The order:

1. nothing stored and offline → `NothingToServe`
2. nothing stored → `Fetch`
3. offline → `ServeCacheStale`
4. within the allowance → `ServeCache`
5. otherwise → `ServeCacheThenFetch`

Offline is checked before age deliberately. With no network, how old the content is changes
nothing about what can be done, and treating it as a refresh case produces a failure the reader
cannot act on.

*Alternative rejected:* a `FreshnessChecker` class holding the clock and the connectivity source.
It reads more like ordinary Android code, and then every test needs two fakes wired before it can
assert anything. A function needs neither.

### The allowance, and the multiplier that was removed

```
allowance = serverMaxAge ?: timeToLive
```

That is the whole formula. For articles it is ten minutes, because the server says
`max-age=600`, and our own figure of fifteen minutes is never used.

An earlier draft multiplied that allowance on a metered connection — four times for a source
that cannot be revalidated cheaply, twice for one that can — and justified it first as saving
data and then, when the arithmetic did not support that, as saving battery. Both are wrong, and
the second is wrong in a way worth writing down.

The data version does not survive pricing: stretching ten minutes to forty saves about 50 KB an
hour of reading, which is one thumbnail. The battery version has a hole that is not about
magnitude at all. **If the reason is battery, the signal is wrong.** An unmetered connection
also wakes a radio and also costs power; metering is about money. Keying a power measure off a
money signal is using one as a proxy for the other, which is exactly the mistake this design
rejects when it insists the enum is named `Unmetered` rather than `WiFi`.

So the multiplier is gone. The connection still changes behaviour, but only where the data
actually is, which is the next section.

Removing it also removed a parameter. `RevalidationCost` existed only to feed the multiplier, and
for the one source built here its value is fixed, so it would have been a parameter with one
possible value — a comment wearing a type. The measurement that motivated it is real and goes in
the README as what a second source would change.

### The connection governs pictures and look-ahead, and nothing else

Two places spend the reader's data, and both are in the UI:

| Decision | Saver off | Saver on | What it costs |
|---|---|---|---|
| Fetch a picture | on screen and a little ahead | **on screen only, at most two at a time** | about 50 KB each, up to a megabyte a page |
| Start the next page | `prefetchDistance` five | `prefetchDistance` one | 17 KB plus that page's pictures |

```kotlin
// :core:designsystem
enum class DataSaver { Off, On }

val LocalDataSaver = staticCompositionLocalOf { DataSaver.Off }
```

`:app` observes `Connectivity`, maps `Unmetered` to `Off` and everything else to `On`, and
provides it at the root. The article picture composable reads it to decide whether to look ahead;
the feed screen reads it to pick the `prefetchDistance` it builds its `PagingConfig` with.

`DataSaver` lives in `:core:designsystem` rather than making the design system depend on
`:core:freshness`, because the design system is the UI's vocabulary and freshness is a data-layer
policy. The mapping belongs in `:app`, the one module that already sees everything — the same
reason the navigation graph lives there.

It is a composition local rather than a parameter threaded through every card because an image
loading policy is ambient in the way a theme is. `staticCompositionLocalOf` because the value
almost never changes.

### Connectivity behind an interface

```kotlin
// :core:freshness
interface Connectivity {
    fun current(): Connection
    fun observe(): Flow<Connection>
}
```

The implementation lives in `:core:network` and reads `NET_CAPABILITY_NOT_METERED` from
`NetworkCapabilities`, not the transport type. Android models metering as a capability separate
from the transport and lets the reader mark a Wi-Fi network as metered; a phone tethering another
phone is Wi-Fi and metered, an unlimited mobile plan is cellular and unmetered. The two decisions
above care whether bytes cost money, so that is what the name says.

`observe()` exists so the feed can drop its out-of-date marker when the network returns.

### Room is the only thing the UI reads

```
Retrofit ──writes──► Room ──Flow──► repository ──Flow──► view model ──► screen
```

Nothing returns network data to the caller. A fetch writes to Room and returns nothing but
success or failure; the UI updates because Room emits. A failed refresh cannot empty the screen,
the offline case needs no separate branch, and the detail screen needs no network at all, which
is what will make a saved article readable offline in the next slice.

```kotlin
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val imageUrl: String?,
    val publishedAt: Instant,
)

@Entity(tableName = "feed_metadata")
data class FeedMetadataEntity(
    @PrimaryKey val id: Int = 0,     // there is only ever one row
    val lastRefreshedAt: Instant,
    val serverMaxAge: Duration?,
    val nextOffset: Int,
    val hasMore: Boolean,
)
```

The list is `SELECT * FROM articles ORDER BY publishedAt DESC`, returning a
`PagingSource<Int, ArticleEntity>` generated by `room-paging`. An article carries no page index,
because a page is how we fetched, not what the article is, and Paging works out the windows
itself.

That absence is the point. With a page index the key has to be the article and the page it
arrived in, so the same article fetched at two offsets becomes two rows and appears twice.
Ordering by published time, a property of the article that never changes, makes fetching a pure
side effect: how many times we fetch, and in what order, does not change the result.

### What `:components:feed:domain` does now

Paging carries the articles, so the feed is no longer one composed list. It is one paged stream
plus a number of sections above it, each from its own source with its own allowance:

```kotlin
// :components:feed:domain — plain Kotlin, and no paging dependency
sealed interface FeedSection {
    data class Weather(val weather: com.okensun.todayfeed.components.weather.api.Weather) : FeedSection
}

class ObserveFeedSections @Inject constructor(
    private val weather: WeatherRepository,
) {
    operator fun invoke(): Flow<List<FeedSection>>
}
```

Its job is the ordered list of non-paged sections and what to do when one of them has nothing.
Adding a source later is additive: a case in the sealed type and a constructor parameter, no
change to the type it returns.

The screen then puts sections above the paged articles:

```kotlin
LazyColumn {
    sections.forEach { section -> item { Section(section) } }
    items(paged.itemCount) { ArticleRowCard(paged[it]) }
}
```

Two consequences worth naming. The sections and the articles **never meet**, so "one source
failing must not empty the feed" stops being a rule to maintain and becomes something that cannot
be violated. And "the feed is empty" now belongs to the article list alone, so a weather card with
no articles shows the weather card rather than an empty state — which is the better behaviour and
was not what the combined version did.

This module is thin while weather is the only section. It stays because its job is stated in terms
of the list rather than the count, so a second section costs nothing, and because the alternative
is putting the ordering rule in a composable where a unit test cannot reach it.

### Pagination is Paging 3, and the policy sits in the seam it provides

```
Retrofit ──► RemoteMediator ──► Room ──► PagingSource ──► PagingData ──► LazyPagingItems
                    ▲                    (room-paging)
                    │
              decide(...) in initialize()
```

Paging 3 owns the offset, the append and prepend orchestration, the retry, the end-of-pagination
signal, and the trigger that starts the next page. `room-paging` generates the `PagingSource`
from a `@Query`. None of that is interesting code and all of it is the code most likely to have
an edge case in it.

The freshness decision stays ours, in the place the library provides for it:

```kotlin
override suspend fun initialize(): InitializeAction {
    val decision = decide(
        cachedAt = metadata.lastRefreshedAt,
        serverMaxAge = metadata.serverMaxAge,
        timeToLive = ARTICLE_TTL,
        connection = connectivity.current(),
        now = clock.instant(),
    )
    return if (decision is Decision.ServeCache) InitializeAction.SKIP_INITIAL_REFRESH
    else InitializeAction.LAUNCH_INITIAL_REFRESH
}
```

`initialize()` exists precisely so an app can consult its own cache before the pager decides to
refresh. So "the paging is the library's, the freshness is ours" is not a compromise; it is the
split the library was designed for.

**Refresh keeps the reader's place.** The `REFRESH` branch of `load()` upserts and does not
delete. Room's write invalidates the `PagingSource`, and Paging reloads around
`PagingState.anchorPosition` rather than from the top.

**Refresh reaches back.** The `REFRESH` branch loops: fetch a page, upsert it, and stop when the
page contains an article already stored or after five pages. This source publishes twenty to
forty articles a day and the allowance only applies while the app is in use, so a reader who
opens the app daily is more than a page behind every time. Fetching only the newest page would
leave a day missing in the middle with nothing said. Fetching several pages inside one `load()`
is unusual for Paging but perfectly legal: `load()` writes to the database and the database is
what pages.

**The trigger distance is `PagingConfig.prefetchDistance`**, which is the parameter the library
already has for what would otherwise have been hand-written. Its value comes from `DataSaver`.

**One thing needs a second entry point.** `initialize()` runs once, when the pager starts
collecting. "The app came back to the foreground after twenty minutes" has to re-evaluate, so the
screen calls a policy-gated refresh for that case. Two places consult the policy rather than one.
That is a wart, and it is smaller than a paging state machine.
### How staleness reaches the UI — slice 1's open question

`ContentState` does not change, and no field is added to it. The feed derives it from what Paging
reports, in a pure function:

```kotlin
// :components:feed:ui
internal fun feedContentState(
    refresh: LoadState,
    itemCount: Int,
    offline: Boolean,
    cached: List<FeedItem>?,
): ContentState<List<FeedItem>> = when {
    itemCount > 0 && offline -> ContentState.Offline(cached)
    itemCount > 0            -> ContentState.Content(cached.orEmpty())
    refresh is LoadState.Loading -> ContentState.Loading
    offline                  -> ContentState.Offline(null)
    refresh is LoadState.Error -> ContentState.Error(refresh.error.message.orEmpty())
    else                     -> ContentState.Empty
}
```

`itemCount > 0` comes first, which is how "a failed refresh must not empty the screen" is
expressed. It is not a rule anyone has to remember; it is the order of the conditions. The spike
verified that a plain unit test drives every branch.

The refreshing and appending indicators come from `LoadState` directly in the screen, so there is
no `FeedUiState` wrapper and no `FeedState` record in `api` any more. Both existed to carry
paging state the library now reports itself.

*Alternative rejected:* a `stale` flag on `ContentState.Content`. It would put a data-layer
concern into the shared UI vocabulary four other screens use, to serve one screen.
### Why Paging 3, after being wrong about it three times

The first version of this design hand-wrote the paging and gave two reasons. Both were false, and
so was a third objection raised later. They are recorded because the pattern in them is more
useful than the conclusion.

| Claim | What is actually true |
|---|---|
| `RemoteMediator` takes the load decision away, so the policy cannot be tested | `initialize()` exists for exactly that decision, and a `RemoteMediator` is an ordinary class a test can drive |
| Refresh invalidates and restarts, which loses the reader's place | The `REFRESH` branch need not delete. Paging reloads around `anchorPosition`, so the place is kept |
| `PagingData` is a diff stream, so a JVM view test cannot drive a paged screen | `PagingData.from(items, sourceLoadStates = ...)` exists for tests and previews |

Each of those was checked by a throwaway spike rather than argued: `room-paging` generates its
`PagingSource` under KSP on Kotlin 2.3, `initialize()` accepts the decision, a Robolectric view
test renders a `PagingData` flow, and the four content states come out of a plain function over
`loadState` and `itemCount`. Five spike tests, all passing, then deleted.

The pattern worth keeping: all three claims were of the form "the library cannot do X", and the
accurate statement was "the library does X differently, and the difference has a specific shape".
Looking for a reason to keep a decision already made produces "cannot" rather than "how".

What the choice actually costs:

- **`ContentState` is derived rather than handed down** for the feed, while the other screens map
  it in a view model. One type, two sources. The derivation is a pure function and is unit
  tested, so this is smaller than it first looked.
- **Interleaving is constrained.** A card placed between articles needs
  `PagingData.insertSeparators`, whose generator must be a pure function of the two adjacent
  items. "Insert on a day boundary" is direct; "insert every tenth article" needs the article to
  carry a stable ordinal, because the generator gets no index and may be called more than once
  for the same boundary. Nothing in this slice interleaves, and the note is in `DECISIONS.md` for
  whoever adds a promotional card later.
- **Fetching several pages inside one `load()`** is legal but unusual, and a reviewer who knows
  Paging will look twice at it. The comment there says why.

What it buys is not really time. Honest estimate: about half an hour, because the wiring costs
some of what the machinery saves. What it removes is variance — the paging state machine was the
single item most likely to overrun, and it is now the library's problem.
### Where the refresh triggers live

The view model, not the repository. `onResumed()`, `onRefresh()` and `onApproachingEnd()` are
called by the screen; the repository has no idea what a lifecycle is. The repository answers
"refresh if the policy says so" and "load the next page".

Pull to refresh bypasses the policy, because the reader asking is a stronger signal than any
allowance.

### The claims about network use are measured

The fake article source counts requests and reports the bytes it would have returned. That turns
the README's claims into assertions:

| Situation | Requests | Asserted in |
|---|---|---|
| second open within the allowance | 0 | policy test, and a mediator test asserting `SKIP_INITIAL_REFRESH` |
| open with a warm cache and no network | 0 | mediator test |
| scroll three pages, saver off | 3 article requests | mediator test |
| scroll three pages, saver on | 3 article requests, pictures only for cards shown | mediator and view tests |
| refresh a day behind | at most 5 page requests | mediator test |
| refresh when up to date | exactly 1 | mediator test |

An earlier draft of this plan had no numbers in it anywhere, which is how the two bad arguments
above survived as long as they did.

## Risks / Trade-offs

- **The estimate.** About seven and a quarter hours of work against a four hour block. Handled by
  the shape of the task list rather than by hoping: five passes, each ending with the app working
  and better than before, and a stated cut order.
- **Fetching several pages inside one `load()`** is legal but unusual, and it is where a bug
  would hide. → It stops on the first page containing something already stored, and it is capped
  at five. Both are unit tested with a counting fake.
- **Two entry points consult the policy**, `initialize()` and the foreground refresh. → They call
  the same function with the same inputs, so they cannot disagree; only the trigger differs.
- **`ContentState` for the feed is derived rather than handed down**, while other screens map it
  in a view model. One type, two sources. → The derivation is a pure function, unit tested, and
  the spike verified every branch.
- **Interleaving is constrained** by `insertSeparators` needing a pure function of adjacent items.
  → Nothing in this slice interleaves. Noted in `DECISIONS.md` for whoever adds a promotional
  card.
- **Pictures make the list heavier**: twenty thumbnails is memory and decode work on every scroll.
  → The card shows text immediately and the picture arrives when it arrives, so a slow host never
  blocks reading.
- **The metered behaviour is hard to see on an emulator.** → The allowance is a parameter of a
  pure function. `DataSaver` reaching the picture composable and the `prefetchDistance` is checked
  by hand once, on a device, on mobile data. The request counts are asserted with a counting fake.
- **Version compatibility, which was a real risk, is retired.** A spike verified `room-paging`
  under KSP on Kotlin 2.3 with AGP 9, `initialize()`, `PagingData` in a Robolectric view test, and
  the state derivation. Three of today's four version questions turned out badly; this one did
  not, and it was checked rather than assumed.

## Open Questions

- Whether the out-of-date marker is a banner above the list or a mark on each card. Cosmetic.
- The page size, twenty, and the refresh cap, five pages. Both are constants and neither changes
  the design. Twenty times five is a hundred articles, which is about three days of publishing.
