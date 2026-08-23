## Why

The brief asks for a feed that "stays reasonably fresh without wasting the user's mobile
data", and for saved items to be readable offline. Those two sentences are the hardest part of
the assignment, and everything built so far only prepares for them. The feed currently shows a
fixed list from memory.

This change replaces that with the real thing: articles from the network, stored locally, and
a policy that decides when it is worth asking for more. The policy is the part the brief
weighs, so it is built first and on its own, before any network or storage code exists to
entangle it.

## What Changes

### The policy, in `:core:freshness`

A plain Kotlin module with no Android on its classpath. It answers two separate questions, and
keeping them separate is the point:

- **When is it worth asking?** A time-to-live per source. Where the server states its own
  `Cache-Control: max-age`, that wins over ours.
- **What does asking cost?** Whether the source answers `304 Not Modified` with an empty body.

Both were measured rather than guessed. Spaceflight News states `max-age=600` and cannot be
revalidated cheaply. That combination is the interesting one: the source that changes fastest
is also the one where every check costs a full response.

A metered connection lengthens every time-to-live, and lengthens it further for a source that
cannot be revalidated cheaply. Connectivity arrives as an injected interface, time arrives as
an injected clock, so the whole thing is a function with no hidden inputs.

The answer is one of four cases rather than a yes or no:

| Case | Meaning |
|---|---|
| `ServeCache` | young enough that asking would change nothing |
| `ServeCacheAndRevalidate` | old, but we have something. Show it now, refresh behind it |
| `FetchBlocking` | nothing stored yet, so the user has to wait |
| `ServeStaleOffline` | no network. Show what we have and say it may be old |

### The data layer, in `:components:articles:data`

Retrofit against `https://api.spaceflightnewsapi.net/v4/articles/`, Room as the single source
of truth, and pagination written by hand. The network writes to Room and the UI only ever
reads from Room, so what the user sees is always what is stored. Each stored page records when
it was written, which is what gives the policy an age to work from.

The repository implements the `ArticleRepository` interface that already exists in
`:components:articles:api`, so nothing above the data layer changes shape. The in-memory
implementation is deleted.

### The screens

Real articles instead of placeholders, each with the picture the source provides: a thumbnail on
the card and a wide one at the top of the detail screen. The four content states become real
instead of hard-coded. Pull to refresh. The next page starts loading a few articles before the
reader reaches the end, so the list does not stall. An honest marker when what is shown may be
old because the device is offline. The detail screen reads its article from the cache, which is
what will make a saved article readable offline in the next slice.

Refresh happens only at moments the user can see: the app coming to the foreground, a tab being
shown, a pull to refresh, and approaching the end of the list. No background work of any kind.

A metered connection changes three things, not one: how long the app tolerates old articles,
whether it fetches pictures for cards the reader has not reached, and how early it starts the
next page. Those are the three places data is actually spent.

### Left out on purpose

The weather component, save and unsave, the Saved screen's real contents, the movie component
and search. They are the next slice or were cut for time. Nothing here touches them.

**Trade-off, stated plainly.** Pagination is written by hand rather than using Paging 3, which
is the official answer and would handle the edge cases for us. Paging 3's `RemoteMediator`
decides when a load happens, and that decision is exactly what this change is about. Keeping it
in our own code is what makes it a unit test against a fake clock instead of a framework
behaviour we hope is right. The cost is that the paging state machine, its retry and its
end-of-list handling are ours to get right.

The second trade-off is what the metered connection is allowed to change, and getting this
honest took a rewrite. Stretching the refresh interval from ten minutes to forty saves about 50
KB an hour of reading, which is one thumbnail. So the longer interval is not a data measure at
all — what it saves is radio wakeups, and therefore battery. The data is in the pictures, which
are around fifty times the text, so that is where a metered connection has to be allowed to
change behaviour. Both levers stay, described as what they each actually do.

The simpler option was one interval and no connection awareness anywhere. Rejected because the
brief asks about the reader's mobile data, and answering it with a number that saves a thumbnail
an hour would not survive being priced.

Satisfies from the brief: the *paginated feed*, the *freshness policy* including the write-up,
and *all four UI states handled explicitly* for real rather than in previews. Prepares the
*saved items readable offline* must-have by making the cache the only thing the UI reads.

## Capabilities

### New Capabilities
- `article-feed`: reading a paginated list of articles that keeps working without a network,
  refreshes itself only when that would change what the reader sees, and says so when what is
  shown may be old.

### Modified Capabilities

None. `app-shell` describes navigation and the shape of the four states, and neither changes.

## Impact

- **New**: `:core:freshness` gains the policy and its tests; `:components:articles:data` gains
  a Retrofit service, Room entities, a DAO, a database and the real repository;
  `:core:testing` gains a fake connectivity source.
- **Deleted**: the in-memory article repository, and the placeholder state in the feed and
  detail view models.
- **Dependencies added**: none beyond what the version catalog already declares. Retrofit,
  OkHttp, Room and serialization are already there and reach the data layer through the
  `todayfeed.data` convention plugin.
- **Risk**: the paging state machine is the part most likely to take longer than planned. If
  the timebox runs out, the policy and its tests must already be finished, because that is what
  the brief weighs. The order in the task list is also the priority order.
- **User-visible behaviour**: the feed shows real articles, keeps showing them without a
  network, loads more as the reader scrolls, and admits when it may be out of date.
