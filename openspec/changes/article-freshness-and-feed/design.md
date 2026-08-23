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
| Start the next page | five articles from the end | one article from the end | 17 KB plus that page's pictures |

```kotlin
// :core:designsystem
enum class DataSaver { Off, On }

val LocalDataSaver = staticCompositionLocalOf { DataSaver.Off }
```

`:app` observes `Connectivity`, maps `Unmetered` to `Off` and everything else to `On`, and
provides it at the root. The article picture composable reads it to decide whether to look ahead;
the feed screen reads it to pick its trigger distance.

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

The list is `SELECT * FROM articles ORDER BY publishedAt DESC`. An article carries no page index,
because a page is how we fetched, not what the article is.

That absence is the point. With a page index the key has to be the article and the page it
arrived in, so the same article fetched at two offsets becomes two rows and appears twice.
Ordering by published time, a property of the article that never changes, makes fetching a pure
side effect: how many times we fetch, and in what order, does not change the result.

### Pagination, written by hand

Refresh fetches from offset zero. Load-more fetches from `nextOffset`. Both upsert on the article
id, so neither has to know what the other did, and refresh keeps the pages the reader has already
scrolled through.

The source pages by offset and orders newest first, so offsets drift: publish three articles and
what was at offset twenty is now at twenty-three. The two operations react to that differently.

| Operation | After three new articles | Result |
|---|---|---|
| Load-more | `nextOffset` twenty now points at what was seventeen | three articles already held are fetched again and the upsert absorbs them. **A gap is impossible** |
| Refresh | offset zero is a page of the newest | everything published since the last refresh beyond one page is missed unless refresh keeps going |

**Refresh pages forward until it meets what it already has.** An earlier draft fetched only page
zero, recorded a conservative warning when every article in it was new, and put the rest in the
README as a limitation. That was wrong, and the reason is in the Context: this source publishes
twenty to forty articles a day, and the ten-minute allowance only applies while the app is in
use. A reader who opens the app once a day is more than a page behind **every time**. The gap was
not an edge case, it was the normal case, and the reader would have scrolled from today straight
into yesterday with a day missing in between and nothing said.

So refresh is a loop: fetch a page, upsert it, and stop when the page contains an article already
stored or when a cap is reached. The cap is five pages, which covers a hundred articles, about
three days of publishing. Past that the reader is better served by the newest hundred than by a
long wait, and the README says so.

Load-more triggers before the end rather than at it. Firing when the reader reaches the last
article means watching a spinner, and article twenty-one always waits. How early depends on
`DataSaver`.

Refresh and load-more are serialised behind one mutex. Less parallel, much easier to reason
about, and no reader will notice.

### How staleness reaches the UI — slice 1's open question

`ContentState` does not change. The feed screen's state wraps it:

```kotlin
// :components:feed:ui
data class FeedUiState(
    val content: ContentState<List<FeedItem>>,
    val isRefreshing: Boolean,
    val isAppending: Boolean,
    val hasMore: Boolean,
)
```

The repository exposes what the UI needs to build that, in `:components:articles:api`:

```kotlin
data class FeedState(
    val articles: List<Article>,
    val isRefreshing: Boolean,
    val isAppending: Boolean,
    val hasMore: Boolean,
    val mayBeStale: Boolean,      // shown, but the device is offline
    val lastFailure: String?,
)
```

Independent facts, so separate fields rather than a sealed hierarchy that would have to model
impossible combinations.

| `FeedState` | `content` |
|---|---|
| no articles, refreshing | `Loading` |
| no articles, source returned none | `Empty` |
| no articles, a failure | `Error(failure)` |
| no articles, offline | `Offline(null)` |
| articles, offline | `Offline(articles)` |
| articles | `Content(articles)` |

`isRefreshing` and `isAppending` sit outside `ContentState` because they are not states of the
content — they describe work happening while content is already on screen.

*Alternative rejected:* a `stale` flag on `ContentState.Content`. It would put a data-layer
concern into the shared UI vocabulary four other screens use, to serve one screen.

### Why not Paging 3

The first draft said `RemoteMediator` takes the load decision away from us and makes the policy
untestable. That is not true, and the correction matters. `RemoteMediator.initialize()` exists
precisely so an app can consult its own cache timestamp before deciding whether to refresh, and a
`RemoteMediator` is an ordinary class a unit test can drive.

Two reasons that do hold:

**Refresh semantics.** Paging 3 refreshes by invalidating the `PagingSource` and starting again.
The behaviour chosen above — keep every page the reader has loaded, upsert by id, page forward
until we meet known content — is the opposite of invalidate-and-restart. Getting it out of Paging
3 means fighting the model the library is built around.

**`PagingData` does not fit this project's testing.** It is a stream of differences, not a list.
Asserting on it needs `asSnapshot()` or a differ, and `LazyPagingItems` couples the screen to the
library. The project's screens are stateless composables that take a plain state and are checked
by JVM view tests. A `PagingData` screen cannot be tested that way, which would cost the approach
the whole of slice 1's second half was spent establishing.

The cost of the choice is real: the paging state machine, its retry and its end-of-list handling
are ours to get right. They are unit tested rather than tested by scrolling.

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
| second open within the allowance | 0 | policy and repository tests |
| open with a warm cache and no network | 0 | repository test |
| scroll three pages, saver off | 3 article requests | repository test |
| scroll three pages, saver on | 3 article requests, pictures only for cards shown | repository and view tests |
| refresh a day behind | at most 5 page requests | repository test |

An earlier draft of this plan had no numbers in it anywhere, which is how the two bad arguments
above survived as long as they did.

## Risks / Trade-offs

- **The estimate.** Seven and three quarter hours of work against a four hour block. This is
  handled by the shape of the task list rather than by hoping: it is six passes, each of which
  ends with the app working and better than before. See `tasks.md` for the cut order.
- **The paging state machine is the most likely thing to overrun**, especially refresh during an
  append. → Serialised behind one mutex.
- **Room and a hand-written pager mean we own the edge cases** Paging 3 solved. → Argued above,
  and they are unit tested.
- **The refresh loop can make five requests where one would do**, for a reader who opens the app
  every few minutes. → It stops at the first page containing something already stored, so for a
  reader who is up to date it makes exactly one request. The loop only runs long when the reader
  has been away long, which is when they want it to.
- **Pictures make the list heavier**: twenty thumbnails is memory and decode work on every
  scroll. → The card shows text immediately and the picture arrives when it arrives, so a slow
  host never blocks reading.
- **The metered behaviour is hard to see on an emulator.** → `DataSaver` reaching the picture
  composable and the trigger distance is checked by hand once, on a device, on mobile data. The
  request counts are asserted with the counting fake.
- **The policy could grow into a framework.** It is one function and five cases. → It stays that
  until a second source needs something it cannot express.

## Open Questions

- Whether the out-of-date marker is a banner above the list or a mark on each card. Cosmetic.
- The page size, twenty, and the refresh cap, five pages. Both are constants and neither changes
  the design. Twenty times five is a hundred articles, which is about three days of publishing.
