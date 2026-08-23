## Why

The brief asks for a feed that "stays reasonably fresh without wasting the user's mobile data",
and for saved items to be readable offline. Those two sentences are the hardest part of the
assignment, and everything built so far only prepares for them. The feed currently shows a fixed
list from memory.

This change replaces that with the real thing: articles from the network, stored locally, and a
policy that decides when it is worth asking for more.

## What Changes

### The policy, in `:core:freshness`

A plain Kotlin module with no Android on its classpath. One function whose every input is a
parameter — what is stored and when, what the source said about its own maximum age, our own
figure, the connection, and the time — and whose answer needs no further interpretation:

| Answer | Meaning |
|---|---|
| `ServeCache` | young enough that asking would change nothing |
| `ServeCacheThenFetch` | past its allowance, but something is stored. Show that, refresh behind it |
| `Fetch` | nothing stored, so the reader waits |
| `ServeCacheStale` | no network, something stored. Show it and say it may be old |
| `NothingToServe` | no network and nothing stored |

The allowance is the source's own stated maximum age where it gives one, and ours where it does
not. For this source it is ten minutes, because the server says `max-age=600`.

### The data layer, in `:components:articles:data`

Retrofit against `https://api.spaceflightnewsapi.net/v4/articles/`, Room as the single source of
truth, and Paging 3 for the paging. The network writes to Room and the UI only ever reads from
Room, so what the reader sees is always what is stored, and a failed refresh cannot empty the
screen.

The freshness decision stays ours, in `RemoteMediator.initialize()`, which is the seam the library
provides for exactly that. Paging owns the offset, the append orchestration, the retry, the
end-of-pagination signal and the trigger that starts the next page.

Refresh upserts on the article id without deleting, so someone five pages in who pulls to refresh
keeps their place: Paging reloads around where they are rather than from the top. It also **pages forward until it reaches an article
already stored**, capped at five pages, because this source publishes twenty to forty articles a
day and a reader who opens the app daily is more than a page behind every time. Fetching only the
newest page would have left a day missing in the middle with nothing said.

### The screens

Real articles instead of placeholders, each with the picture the source provides: a thumbnail on
the card and a wide one at the top of the detail screen. The four content states become real
instead of hard-coded. Pull to refresh. The next page starts a few articles before the reader
reaches the end, so the list does not stall. An honest marker when what is shown may be old
because the device is offline.

Refresh happens only at moments the reader can see: the app coming to the foreground, a tab being
shown, a pull to refresh, and approaching the end of the list. No background work of any kind.

### What a metered connection changes

Pictures and look-ahead, and nothing else. On a metered connection the app fetches pictures only
for the articles on screen and starts the next page later. **The allowance does not depend on the
connection.**

An earlier draft of this plan stretched the allowance on a metered connection and justified it
first as saving data, then as saving battery. Both were wrong. Ten minutes to forty saves about
50 KB an hour of reading, which is one thumbnail, so it is not a data measure. And if the reason
were battery then the signal is wrong, because an unmetered connection also wakes a radio —
metering is about money. A picture is around 50 KB and a page of text is 17 KB, so the data is in
the pictures, and that is the only place a metered connection is allowed to change behaviour.

### Left out on purpose

The weather component, save and unsave, the Saved screen's real contents, the movie component and
search. They are the next slice or were cut for time. Nothing here touches them.

## How this will be judged, in numbers

The fake article source counts requests, so the claims are assertions rather than prose:

| Situation | Requests |
|---|---|
| a second open inside the allowance | 0 |
| an open with a warm cache and no network | 0 |
| scrolling three pages | 3 |
| a refresh when already up to date | 1 |
| a refresh after a day away | at most 5 |

The first draft of this plan had no numbers in it, which is how two bad arguments survived in it
as long as they did.

## Trade-offs

**Paging 3, after three wrong reasons for avoiding it.** The first version of this plan
hand-wrote the paging, and gave reasons that turned out to be false: that `RemoteMediator` takes
the load decision away and makes the policy untestable, that refresh invalidates and so loses the
reader's place, and that `PagingData` cannot be driven by a JVM view test. `initialize()` exists
for that decision; the refresh branch need not delete, and Paging reloads around the anchor;
`PagingData.from` exists for tests. All three were checked by a throwaway spike rather than
argued, along with `room-paging` under KSP on Kotlin 2.3 and the state derivation. Five spike
tests, all passing, then deleted.

The pattern in those three is more useful than the conclusion: each was of the form "the library
cannot do X", when the accurate statement was "the library does X differently". Looking for a
reason to keep a decision already made produces "cannot" rather than "how".

What it costs: `ContentState` is derived from load states for the feed while the other screens
have it handed down, which is one type from two sources; a card placed between articles would need
`insertSeparators`, whose generator must be a pure function of the two adjacent items; and the
refresh branch fetches several pages in one `load()`, which is legal but unusual. What it buys is
not really time — about half an hour — but the removal of the paging state machine, which was the
single item most likely to overrun.

**The estimate is seven and a quarter hours against a four hour block**, and that is written into the
task list rather than smoothed over. The first draft claimed four hours for the same work, which
was five and a half minutes a task. The response is the shape of the plan rather than optimism:
five passes, each ending with the app working and better than before, and a stated cut order. The
earlier version was horizontal — all of the policy, then all of the storage, then all of the
network — and with that shape, running out of time before the last group leaves an app still
showing placeholders.

## Capabilities

### New Capabilities
- `article-feed`: reading a paginated list of articles that keeps working without a network,
  refreshes itself only when that would change what the reader sees, reaches back far enough not
  to leave a gap, and says so when what is shown may be old.

### Modified Capabilities

None. `app-shell` describes navigation and the shape of the four states, and neither changes.

## Impact

- **New**: the policy and its tests in `:core:freshness`; a Retrofit service, two Room tables, a
  DAO, a `RemoteMediator` and the real repository in `:components:articles:data`;
  `ObserveFeedSections` in `:components:feed:domain`; `FakeConnectivity` and a counting fake
  source in `:core:testing`; `DataSaver` in `:core:designsystem`; Paging 3, `room-paging` and Coil
  in the catalog.
- **Deleted**: the in-memory article repository, `ObserveFeed` and its tests, and the placeholder
  state in the feed and detail view models.
- **Risk**: the refresh loop inside one `load()` call, which is where a bug would hide. It stops
  at the first page containing something already stored and is capped at five.
- **User-visible behaviour**: the feed shows real articles with pictures, keeps showing them
  without a network, loads more before the reader reaches the end, reaches back after a day away,
  and admits when it may be out of date.
