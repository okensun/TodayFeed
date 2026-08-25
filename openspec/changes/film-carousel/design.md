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

**Picked.** Twelve hours, the same shape of number as every other source.

**Considered instead.** Treating the catalogue as permanent: fetch once, never again. Twenty-two
films that were released between 1986 and 2014 will not change while anyone is reading this.

**Trade-off.** "Never changes" is a fact about today's data, not about the contract, and it is not
even true of all of today's data: every film carries `rt_score`, a review score, and a review
score moves. A film added, a director's name corrected or a score revised would leave the app
showing something wrong for as long as it is installed, and nothing in the code would explain
why. Half a day is short enough that a correction reaches a reader who opens the app in the
morning and again after work, and long enough that the app asks about a catalogue at most once a
session. The policy also stays uniform: there is no branch anywhere that means "this one is
special".

The shape of the number is corroborated. Two comparable keyless catalogues state one for
themselves, and both state a day: `itunes.apple.com/search` and `api.tvmaze.com/shows` each send
`max-age=86400`, measured on 2026-08-25. Ours is half of that, because we hold the films in
memory and revalidate nothing, so being early costs one small request and being late shows a
wrong score.

### A stated `no-cache` counts as stating nothing

The source sends `cache-control: no-cache`, which read literally means "ask every time". Our
parser reads `max-age` and finds none, so our own figure applies — and that is the behaviour we
want, so the design states it rather than leaving it as an accident of parsing.

The reason it is right here: `no-cache` on a catalogue this slow is a statement about a CDN,
not about the content. The rule that a source's stated age beats ours is about a source that has
thought about how fast its content moves. This one has not; it has thought about its cache.

Worth noting and not doing: the source answers `304` with an empty body, so revalidating would
cost almost nothing. Using it needs an `OkHttp` cache, which the project does not configure. That
is a real improvement and it is out of scope here.

### Held in memory, like the weather

32 KB that gains a film every few years does not earn a database, an entity, a DAO and a
migration. The cost is that a cold start with no network shows no films, which the spec allows
because the row is a section: `ObserveFeedSections` already leaves out a section with nothing to
say.

This is the second source to make that choice, so the shape is now a pattern rather than a
one-off: **articles are stored because they are the thing the reader came for; sections are held
in memory because a missing section costs a reader nothing.**

### The row is ordered by score, and says so

The catalogue arrives in no useful order. Ordering it by `rt_score` makes the row worth scrolling
and makes the score worth drawing: a number on a card that explains nothing is decoration, and a
row ordered by a number the reader cannot see looks arbitrary. Each needs the other.

No score sorts last rather than as a nought, because "we were not told" is not "the worst film
ever made". Title breaks a tie, so the same answer always draws the row in the same order.

The ordering lives in the repository, so the contract states it once and one test proves it,
rather than every screen that ever draws a film having to remember.

### Naming the bands is what makes it a mixed feed

With one section the feed read as a weather card with articles under it. With two it has to say
which is which, so each band carries a line naming it: `Films`, `Articles`.

The name is the name and nothing else. Adding how the band is ordered was tried and dropped: it
turns a label into a sentence, and it reads as an apology for an order the content should already
make obvious. The scores on the film cards do that work.

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
