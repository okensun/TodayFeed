## Why

The brief asks for a feed that mixes content of different kinds. Today it mixes two: a weather
card and a list of articles. That satisfies the requirement, but only just, and the two look
alike enough that a reader could take them for one thing in two styles.

A film carousel is the third kind, and it is the one that makes the freshness policy's shape
visible. Spaceflight News states its own maximum age and it wins. Open-Meteo states none, so ours
applies. The film catalogue says `no-cache` and changes a few times a decade. Three sources, three
different answers, from one policy.

## What Changes

- A new `movie` component reading the Studio Ghibli film API: twenty-two films, keyless, each
  with a title, a year, a director and a wide banner picture.
- The feed shows the films as a carousel that scrolls sideways, between the weather card and the
  articles.
- The films follow the same freshness policy as everything else. They are not treated as
  permanent, even though today's twenty-two will not change: that is a fact about the data, not
  about the contract.
- The README stops listing the component as cut.

Not in this change: no film detail screen, no saving a film, and no search. The carousel is a
section of the feed and nothing opens from it yet.

## Capabilities

### New Capabilities

- `film-carousel`: showing a row of films in the feed, and what it does when they cannot be
  fetched.

### Modified Capabilities

None. The feed's requirements are written in terms of sections above the articles, and this adds
a second one without changing what the first does.

## Impact

- New modules `:components:movie:{api,data,ui}`, registered in `settings.gradle.kts` and bound
  from `:app`.
- `:components:feed:domain` gains the films as a second source, so `FeedSection` grows a case and
  `ObserveFeedSections` takes a second repository.
- `:components:feed:ui` draws the new card, through the same sanctioned cross-component
  dependency it already uses for articles and weather.
- No new third-party dependency. The source is keyless, so the project still has no secrets.
