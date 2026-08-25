## Context

Two sources exist and each answers the freshness question differently: Spaceflight News states
`max-age=600` and its number wins; Open-Meteo states nothing and ours applies. The film catalogue
is the third case, and the interesting one.

Measured on 2026-08-25: twenty-two films, 32 KB, `cache-control: no-cache`, and an `ETag`.

## Goals

- A third kind of content in the feed, drawn differently enough that nobody mistakes it for a
  styled article.
- The films go through the same policy as everything else, with no special case in it.
- A failure to fetch them costs the reader nothing.

## Non-goals

- A film detail screen, saving a film, or search. The carousel is a section, not a feature.
- Storing the films in a database.

## Decisions

### The films get an ordinary allowance, not "forever"

**Picked.** Twenty-four hours, the same shape of number as every other source.

**Considered instead.** Treating the catalogue as permanent: fetch once, never again. Twenty-two
films that were released between 1986 and 2014 will not change while anyone is reading this.

**Trade-off.** "Never changes" is a fact about today's data, not about the contract. A source that
adds a film, corrects a director's name or changes a picture URL would leave the app showing
something wrong for as long as it is installed, and nothing in the code would explain why. A day
is short enough that a correction lands, and long enough that the app asks about a catalogue at
most once a session. The policy also stays uniform: there is no branch anywhere that means "this
one is special".

### A stated `no-cache` counts as stating nothing

The source sends `cache-control: no-cache`, which read literally means "ask every time". Our
parser reads `max-age` and finds none, so our own figure applies — and that is the behaviour we
want, so the design states it rather than leaving it as an accident of parsing.

The reason it is right here: `no-cache` on a static catalogue is a statement about a CDN, not
about the content. The rule that a source's stated age beats ours is about a source that has
thought about how fast its content moves. This one has not; it has thought about its cache.

Worth noting and not doing: the source answers `304` with an empty body, so revalidating would
cost almost nothing. Using it needs an `OkHttp` cache, which the project does not configure. That
is a real improvement and it is out of scope here.

### Held in memory, like the weather

32 KB that changes a few times a decade does not earn a database, an entity, a DAO and a
migration. The cost is that a cold start with no network shows no films, which the spec allows
because the row is a section: `ObserveFeedSections` already leaves out a section with nothing to
say.

This is the second source to make that choice, so the shape is now a pattern rather than a
one-off: **articles are stored because they are the thing the reader came for; sections are held
in memory because a missing section costs a reader nothing.**

### The carousel is a `LazyRow` inside the feed's `LazyColumn`

A row that scrolls sideways inside a list that scrolls down is the standard shape and Compose
handles the gesture split. The row keeps its own `LazyListState`, so scrolling the feed does not
reset where the reader had got to sideways.

### `FeedSection` grows a case rather than a list of lists

`FeedSection` is already a sealed interface with one case. Films become a second case, and
`ObserveFeedSections` combines two flows instead of mapping one. The order the sections appear in
is the order that function builds them: weather, films, then the articles below.

## Risks

- **Two sources now feed one list.** If either flow fails the whole section list could fail.
  `ObserveFeedSections` must fold a missing source into an absent section rather than an error,
  which is what it already does for weather and what its tests already cover.
- **The carousel adds a second scroll direction to the feed.** Worth checking by hand that a
  sideways drag does not scroll the feed and a vertical drag does not scroll the row.

## Open questions

None.
