# Roadmap and sequencing

Working notes. They will be shortened into the README's **Plan & Sequencing** section. Each
numbered slice is one OpenSpec change in `openspec/changes/`, and lands as several small
commits.

## How the problem was broken down

The brief hides its hardest part in one bullet: a *freshness policy* for a feed that mixes
sources which update at different speeds, and that still works offline. Feed, detail and save
are ordinary work. The caching and freshness layer is where the real judgement is. So the
slices are ordered to build and test that layer early, against one source, before four
sources make it harder.

| # | Change | Covers | Why here |
|---|--------|--------|----------|
| 1 | `bootstrap-project-skeleton` | multi-module layout, dark theme, CI, single-command build | Nothing can be run or reviewed until a fresh copy builds. Deciding the module layout now is also much cheaper than deciding it after feature code has grown across a line we never drew. |
| 2 | `article-feed-offline-first` | paginated feed, all four UI states, offline-first cache, the freshness policy, tests | The heart of the assignment. One source, hand-written pagination, Room as the single source of truth, and the freshness policy with its unit tests. Everything later plugs into this. |
| 3 | `detail-and-save-offline` | detail screen, save and unsave, Saved tab, reading saved items offline | Finishes the core flow the brief asks for. It comes after slice 2 because it reuses the same cache and needs the article body stored at the moment the user saves it. |
| 4 | `heterogeneous-feed-sources` | mixed feed, extra card types, per-source update speeds | Weather hero card (Open-Meteo), service cards (DummyJSON), "on TV today" carousel (TVMaze). Deliberately after slice 2, so each new source is a setting on a policy that already works instead of a new special case. |
| 5 | `search-and-motion` | search and filter, animations and transitions | Nice-to-haves only. Last, because they can be dropped completely without touching a single must-have. |
| 6 | `submission-docs` | README, DECISIONS.md, AI_USAGE.md, known limitations | Written last so the decision log says what actually happened, not what I planned. Notes are kept as I go, so this slice is assembly and not archaeology. |

## Why this order

Risk first, not features first. Slices 1 to 3 cover every must-have. If the week runs out
after slice 3, the submission is still complete and honest. Slices 4 and 5 add the
nice-to-haves, most valuable first. Slice 6 is a fixed cost and cannot be cut. The brief says
judgement counts for more than completeness, so the write-up must not be the thing that gets
squeezed.

## Left out on purpose

Recorded here as they are decided, so the README section is a record and not hindsight. The
reasoning for each goes in `DECISIONS.md`.

- **Paging 3.** Pagination is hand-written instead, so the freshness logic stays testable.
- **Device and UI tests.** The unit tests target the cache, the freshness policy and the async
  logic the brief names. Compose UI tests would cost more than they would prove here.
- **TMDB.** Replaced by TVMaze. TMDB needs an API key, and a submission that asks the reviewer
  to get a secret before it runs works against the single-command rule. TVMaze needs no key
  and changes daily, which gives the freshness policy a third update speed to reason about.
- **A `domain` module for every component.** Only `articles` gets one. The other three have no
  rules worth a module, and a pass-through class is worse than a small inconsistency.
- **Typed project accessors and a mocking library.** String project paths and hand-written
  fakes instead. Both are explained in `DECISIONS.md`.
