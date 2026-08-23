# Roadmap & sequencing

Working notes that will be condensed into the README's **Plan & Sequencing** section.
Each numbered slice is one OpenSpec change (`openspec/changes/<name>/`) and lands as
several logical commits.

## How the problem was broken down

The brief hides its centre of gravity in one bullet: a *freshness policy* for a feed
that mixes sources updating at different cadences, that stays usable offline. Feed →
detail → save is straightforward CRUD-over-HTTP; the freshness/caching layer is where
the judgment actually is. So the slices are ordered to get that layer built and tested
early, on a single source, before the heterogeneity multiplies its complexity.

| # | Change | Satisfies | Why here |
|---|--------|-----------|----------|
| 1 | `bootstrap-project-skeleton` | multi-module, dark theme, CI, single-command build | Nothing can be demoed or reviewed until a clean checkout builds. Doing the module graph and CI first also stops "we'll modularise later" from becoming never. |
| 2 | `article-feed-offline-first` | paginated feed, all four UI states, offline-first cache, freshness core, tests | The graded heart. One source, hand-rolled pagination, Room as single source of truth, and the freshness abstraction with its unit tests. Everything later plugs into this. |
| 3 | `detail-and-save-offline` | detail screen, save/unsave, Saved screen, offline readability | Completes the required core flow. Sits after 2 because it reuses the same cache and needs the article body persisted at save time. |
| 4 | `heterogeneous-feed-sources` | heterogeneous feed, extra cell types, per-source cadence | Weather hero (Open-Meteo), service cards (DummyJSON), movie carousel (TMDB, optional key). Deliberately after 2 so each new source is a *parameter* of a proven freshness policy rather than a new special case. |
| 5 | `search-and-motion` | search/filter, animations/transitions | Pure nice-to-haves. Last because they can be cut wholesale without touching a single must-have. |
| 6 | `submission-docs` | README, DECISIONS.md, AI_USAGE.md, limitations | Written last so the decision log reflects what actually happened, not what was planned. Notes are captured as I go so this is assembly, not archaeology. |

## Order rationale

Risk-first, not feature-first. Slices 1–3 cover every must-have; if the week
evaporates, the submission is still complete and honest. Slices 4–5 add the
nice-to-haves in decreasing order of what the brief rewards. Slice 6 is fixed cost and
non-negotiable — the brief says judgment beats completeness, so the write-up cannot be
the thing that gets squeezed.

## Deliberately deferred / cut

Recorded here as they are decided, so the README section is evidence rather than
hindsight. See `DECISIONS.md` for the reasoning behind each.

- **Paging 3** — hand-rolled pagination instead, to keep freshness logic testable.
- **Instrumented / UI tests** — unit tests target the cache, freshness and async logic
  the brief names; Compose UI tests would cost more than they'd prove here.
