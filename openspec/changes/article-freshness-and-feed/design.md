## Context

The skeleton is in place: fifteen modules, the four content states, the screens, and an
`ArticleRepository` interface in `:components:articles:api` that an in-memory implementation
currently satisfies. `:core:freshness` exists and holds nothing but an injectable clock.

Slice 1's design left one question open on purpose: **how the data layer tells the UI that what
it is holding may be old.** That question is answered here, because the answer belongs with the
cache that produces the staleness.

Four facts about the sources were measured rather than assumed, by calling them:

| Source | States its own max age | Answers 304 | Response size |
|---|---|---|---|
| Spaceflight News | **yes, `max-age=600`** | no | 8.6 KB for 10 articles, so about 17 KB a page |
| Open-Meteo | no | no | small |
| DummyJSON | no | **yes, empty body** | 15 KB |
| Ghibli films | no (`no-cache`) | **yes, empty body** | 32 KB |

Only the first matters for this slice. It is the awkward one: the source that changes fastest is
also the one where every check costs a whole response.

One more measurement changes the shape of the whole design. An article thumbnail from this
source is around 50 KB. A page of twenty articles is 17 KB of text and up to a megabyte of
images. **Images are sixty times the cost of the text**, so any argument about the reader's
mobile data that only talks about refresh intervals is arguing about the rounding error.

See `DECISIONS.md` for the decisions behind the policy — the two dimensions, the refresh
triggers, the metered multiplier and the four decision cases. This document says how they are
built, not why they were chosen.

## Goals / Non-Goals

**Goals:**

- The policy is a pure function with no hidden inputs, testable as a truth table.
- Room is the only thing the UI reads. A failed fetch cannot empty the screen.
- Every scenario in the `article-feed` spec is reachable in a test with fakes, no network.
- The reader never waits on a blank screen while something is stored.

**Non-Goals:**

- No cache eviction. The article table grows; four days of reading is not a problem worth
  solving here, and the next slice needs saved articles kept forever anyway.
- No conditional request support yet. Spaceflight News answers no ETag, so there is nothing to
  send. The policy models the *cost* of asking; the code that sends `If-None-Match` arrives
  with a source that supports it.
- No image transformations or a custom loader. Coil, one thumbnail on a card and one wide image
  on the detail screen. The source's image URLs point at arbitrary hosts with no size
  parameters, so there is no smaller variant to ask for; the only levers are whether to
  download and whether to look ahead.
- No search, no save, no weather. Later or cut.

## Decisions

### The policy is one function over a small record

```kotlin
// :core:freshness
enum class Connection { Unmetered, Metered, Offline }

/** How cheap it is to ask whether a source has changed. */
enum class RevalidationCost { Cheap, Full }

data class FreshnessPolicy(
    val timeToLive: Duration,
    val revalidation: RevalidationCost,
)

sealed interface Decision {
    data object ServeCache : Decision
    data object ServeCacheAndRevalidate : Decision
    data object FetchBlocking : Decision
    data object ServeStaleOffline : Decision
}

fun decide(
    cachedAt: Instant?,          // null when nothing is stored
    serverMaxAge: Duration?,     // from Cache-Control, when the source states one
    policy: FreshnessPolicy,
    connection: Connection,
    now: Instant,
): Decision
```

Everything the decision depends on is a parameter. There is no clock read inside, no
connectivity check inside, no repository reached into. That is what makes the test a table
rather than a setup.

*Alternative rejected:* a `FreshnessChecker` class holding the clock and the connectivity
source. It reads more like normal Android code, and every test then needs two fakes wired
before it can assert anything. A function needs neither.

### The order the function decides in

1. Nothing stored and offline → `ServeStaleOffline` with nothing to serve. The caller turns
   that into the offline state.
2. Nothing stored, network available → `FetchBlocking`.
3. Something stored and offline → `ServeStaleOffline`.
4. Something stored, age within the allowance → `ServeCache`.
5. Something stored, past the allowance → `ServeCacheAndRevalidate`.

Offline is checked before age on purpose. When there is no network, how old the content is
changes nothing about what can be done, and treating it as a refresh case would produce a
failure the reader can do nothing about.

### The allowance, and where its numbers come from

```
allowance = (serverMaxAge ?: policy.timeToLive) × multiplier(connection, revalidation)

multiplier(Unmetered, _)     = 1
multiplier(Metered, Cheap)   = 2
multiplier(Metered, Full)    = 4
multiplier(Offline, _)       = not used, offline is decided before age
```

For articles that means ten minutes on an unmetered connection, because the server says so, and
forty minutes on a metered one.

**The reason is battery, not bytes.** That is worth being exact about, because the obvious
argument does not hold. A page is 17 KB, so going from ten minutes to forty saves about 50 KB an
hour of reading — which is one thumbnail. Anyone who priced it would notice. What the longer
allowance actually saves is radio wakeups: each refresh brings the cellular radio up out of idle
and holds it there, and on a metered connection that is the expensive part. Fewer wakeups is a
real benefit; "saves data" would have been a claim the numbers contradict.

The multiplier for a cheaply checked source is only 2, because there a check is a couple of
hundred bytes of headers and the radio is up for a moment rather than a transfer.

*Alternative rejected:* one allowance regardless of connection. Simpler, and no reviewer would
call it wrong. It is rejected because the brief asks about the reader's mobile data, and a
policy that never looks at the connection cannot answer that. What answers the data half is the
image policy below; this multiplier answers the battery half.
### The server's stated age wins over ours

`Cache-Control: max-age` is read off the response and stored next to the page. When it is
present it replaces our declared time-to-live. Our figure is the fallback for a source that
states nothing.

This matters more than it looks. Fifteen minutes for articles was my own guess, and the server
says ten. A number the source tells us is a fact; a number I chose is taste. Where both exist,
the fact wins.

### Room is the only thing the UI reads

```
Retrofit ──writes──► Room ──Flow──► repository ──Flow──► view model ──► screen
```

Nothing returns network data to the caller. A fetch writes to Room and returns nothing but
success or failure; the UI updates because Room emits. Three things follow: a failed refresh
cannot empty the screen, the same code path serves the offline case with no branching, and the
detail screen needs no network at all, which is what makes a saved article readable offline in
the next slice.

Two tables, and both are smaller than they first looked:

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

The list is `SELECT * FROM articles ORDER BY publishedAt DESC`. There is no page index on an
article, because a page is how we fetched, not what the article is.

That absence is the whole point. With a page index the primary key has to be the article and the
page it arrived in, and the same article fetched twice at different offsets becomes two rows and
appears twice on screen. Ordering by published time, which is a property of the article and
never changes, makes fetching a pure side effect: how many times we fetch, and in what order,
does not change the result.

*Alternative rejected:* keeping the fetch timestamp in `DataStore` or shared preferences. Fewer
moving parts, but then the age and the data can disagree — the age says fresh while a failure
has emptied the table. In one database they move in one transaction.
### Pagination, written by hand

Refresh fetches offset zero and writes it. Load-more fetches `nextOffset` and writes that. Both
are an upsert keyed on the article id, so neither has to know what the other did. Refresh does
not drop the pages the reader has already scrolled through: someone five pages in who pulls to
refresh keeps their place, which is what every real feed does.

The source pages by offset and orders newest first, so **offsets drift**: publish three articles
and what was at offset twenty is now at twenty-three. The two operations react to that very
differently, and the asymmetry is worth stating because it is the opposite of what offset paging
usually gives you.

| Operation | After three new articles | Result |
|---|---|---|
| Load-more | `nextOffset` twenty now points at what was seventeen | three articles we already hold are fetched again, and the upsert absorbs them. **A gap is impossible** |
| Refresh | only offset zero is fetched | if more than a page was published since the last refresh, the articles in between are never seen |

Gaps only come from refresh, and they can be noticed. New articles arrive at the front, so if
even one article in the refreshed page zero is already stored, everything between is stored too
and there is no gap. Only a page where *every* article is new leaves room for one — and that can
also just mean exactly a page was published. So it is a conservative warning rather than a
verdict, which is how the README describes it.

**Load-more triggers before the end, not at it.** Firing when the reader reaches the last item
means they watch a spinner and item twenty-one always waits. The trigger is a few items short of
the end, so the next page is usually already there. How few depends on the connection, for the
reason in the section below.

Refresh and load-more are serialised behind one mutex. They cannot interleave, which is less
parallel and much easier to reason about, and no reader will notice.
### How staleness reaches the UI — slice 1's open question

`ContentState` stays exactly as it is: the shared four-state vocabulary, nothing added. The feed
screen's state wraps it rather than replacing it:

```kotlin
// :components:feed:ui
data class FeedUiState(
    val content: ContentState<List<FeedItem>>,
    val isRefreshing: Boolean,
    val isAppending: Boolean,
    val hasMore: Boolean,
)
```

The view model maps `FeedState` to that:

| `FeedState` | `content` |
|---|---|
| articles empty, refreshing, nothing stored | `Loading` |
| articles empty, source returned nothing | `Empty` |
| articles empty, a failure | `Error(failure)` |
| articles empty, offline | `Offline(null)` |
| articles present, offline | `Offline(articles)` |
| articles present | `Content(articles)` |

`isRefreshing` and `isAppending` sit outside `ContentState` because they are not states of the
content — they describe work happening while content is already on screen. Folding them in would
mean either a `Content` case with flags on it, which is the same thing with worse spelling, or a
`RefreshingWithContent` case, which multiplies the cases the `when` has to handle.

*Alternative rejected:* adding a `stale: Boolean` to `ContentState.Content`. It would put a
data-layer concern into the shared UI vocabulary that four other screens use, to serve one
screen.

### Connectivity behind an interface, with three consumers

```kotlin
// :core:freshness
/**
 * What a byte costs right now: nothing, money, or it cannot be had.
 */
enum class Connection { Unmetered, Metered, Offline }

interface Connectivity {
    fun current(): Connection
    fun observe(): Flow<Connection>
}
```

The implementation lives in `:core:network` and reads `NET_CAPABILITY_NOT_METERED` from
`NetworkCapabilities`. `:core:freshness` sees only the interface, so it stays plain Kotlin and
the fake in `:core:testing` is a few lines. `observe()` exists so the feed can drop its
out-of-date marker when the network returns, which is a scenario in the spec.

**`Unmetered`, not `WiFi`.** Android models metering as a capability separate from the transport,
and lets the user mark a Wi-Fi network as metered. A phone tethering another phone is Wi-Fi and
metered; an unlimited mobile plan is cellular and unmetered. None of the three decisions below
cares whether the radio is Wi-Fi or LTE — they care whether bytes cost the reader money, and
that is what the name says. Naming it after the transport would be a name that is usually right,
which is the worst kind.

Three places spend the reader's data, and one input governs all three:

| Decision | Unmetered | Metered | Cost of getting it wrong |
|---|---|---|---|
| How often to ask | ten minutes | forty minutes | 17 KB, and a radio wakeup |
| Whether to fetch an image | on screen and a little ahead | **on screen only, at most two at a time** | about 50 KB each, up to a megabyte a page |
| How early to fetch the next page | five items from the end | one item from the end | 17 KB plus that page's images |

The first goes through the policy, because it belongs to the data layer. The other two are UI
decisions, and they share one value rather than three:

```kotlin
// :core:designsystem
enum class DataSaver { Off, On }

val LocalDataSaver = staticCompositionLocalOf { DataSaver.Off }
```

`:app` observes `Connectivity`, maps `Unmetered` to `Off` and everything else to `On`, and
provides it at the root. The article image composable reads it to decide whether to look ahead;
the feed screen reads it to pick its trigger distance.

Two things about that shape. `DataSaver` lives in `:core:designsystem` rather than making the
design system depend on `:core:freshness`, because the design system is the UI's vocabulary and
freshness is a data-layer policy; the mapping belongs in `:app`, the one module that already sees
everything. And it is a composition local rather than a parameter threaded through every card,
because an image loading policy is ambient in exactly the way a theme is. `staticCompositionLocalOf`
because the value almost never changes and no reader needs fine-grained recomposition on it.

*Alternative rejected:* three separate parameters, one per decision. They all branch on the same
question and differ only in their numbers, so the numbers stay with their consumers and only the
question is shared. A fourth consumer then costs nothing.
### Where the refresh triggers live

The view model, not the repository. `onResumed()`, `onRefresh()` and `onReachedEnd()` are called
by the screen; the repository has no idea what a lifecycle is. The repository's job is to answer
"refresh if the policy says so" and "load the next page".

Pull to refresh bypasses the policy, because the reader asking is a stronger signal than any
time-to-live.

## Risks / Trade-offs

- **The paging state machine is the most likely thing to overrun.** Refresh during an append, an
  append during a refresh, and a failure in either. → Requests are serialised behind one mutex,
  so they cannot interleave.
- **Room and a hand-written pager mean I own the edge cases** Paging 3 already solved. →
  Accepted deliberately, and argued in `proposal.md`. They are unit tested rather than tested by
  scrolling.
- **A refresh can leave a gap** when more than a page was published since the last one. → It can
  be noticed, conservatively, and it goes in the README limitations. Fixing it properly means
  paging forward from the newest article we hold until we meet it, which is more machinery than a
  feed refreshed every ten minutes needs.
- **Images make the list heavier.** Twenty thumbnails is memory and decode work on every scroll,
  and a slow host stalls a card. → Coil handles the caching and the placeholder; the list shows
  text immediately and the image arrives when it arrives, so a slow image never blocks reading.
- **The metered behaviour is hard to see on an emulator.** → The allowance is a parameter of a
  pure function, so the truth table covers it. `DataSaver` reaching the image composable and the
  trigger distance is checked by hand once, on a device, with mobile data.
- **Forty minutes on a metered connection may read as too stale.** → It is a number with a
  reason, and the reason is battery rather than bytes. If the reason is disagreed with, it is one
  constant.
- **The policy could grow into a framework.** It has four cases and one function; the temptation
  is a cache-key abstraction and per-request overrides. → It stays a function over a record until
  a second source needs something it cannot express.

## Open Questions

- Whether the out-of-date marker is a banner above the list or a mark on each card. Cosmetic, and
  it does not change the state that drives it.
- The page size. Twenty is the starting figure. It trades how often the reader reaches the end
  against how much is fetched for a page they may not finish, and the trigger distance above
  interacts with it. Both are constants and neither changes the design.
