# Roadmap and sequencing

Working notes. They become the README's **Plan & Sequencing** section. Each numbered slice is
one OpenSpec change in `openspec/changes/`, and lands as several small commits.

## How the problem was broken down

The brief hides its hardest part in one bullet: a *freshness policy* for a feed that mixes
sources which update at different speeds, and that still works offline. Feed, detail and save
are ordinary work. The caching and freshness layer is where the real judgement is. So the
slices are ordered to build and test that layer early, against one source, before more sources
make it harder.

| # | Change | Covers | Why here |
|---|--------|--------|----------|
| 1 | `bootstrap-project-skeleton` | multi-module layout, dark theme, CI, single-command build | Nothing can be run or reviewed until a fresh copy builds. Deciding the module layout now is far cheaper than deciding it after feature code has grown across a line we never drew. |
| 2 | `article-feed-freshness` | the freshness policy, paginated feed, offline-first cache, all four UI states, unit tests | The heart of the assignment. `:core:freshness` first, tests first inside it, then one source wired through Room as the single source of truth. |
| 3 | `detail-save-and-weather` | detail screen, save and unsave, Saved tab, offline reading, the weather hero card | Finishes every must-have. Save needs the article body stored at the moment the user saves it. The weather card is what makes the feed heterogeneous, and it is also the short-TTL end of the freshness policy. |

The submission documents are not a slice. They are written in the last block, before any
optional work, and are never cut.

## Why this order

Risk first, not features first. The three slices cover every must-have. Optional work comes
after the documents, so the thing that gets dropped when time runs out is a feature and never
the write-up. The brief says judgement counts for more than completeness.

## Schedule

Deadline: Thursday 2026-08-27. About sixteen working hours, in four blocks.

| Block | Work | Checkpoint |
|---|---|---|
| Sunday evening, 4h | Slice 1: skeleton, 15 modules, CI, theme, navigation shell | If the convention plugins are not working after 2.5h, take the escape hatch and use plain per-module setup |
| Monday evening, 4h | Slice 2: `:core:freshness` with tests first, then the article feed | The freshness tests must be green by the end of this block. If not, drop optional work and finish here on Tuesday |
| Tuesday evening, 4h | Slice 3: detail, save, Saved tab, weather hero card | Every must-have is done by the end of this block |
| Wednesday evening, 4h | Documents first: README, `DECISIONS.md` final pass, `AI_USAGE.md`. Then the `movie` component only with time left over | Documents are finished before any optional feature is started |
| Thursday | Fresh clone, one command, install, walk through, submit | No new features |

## Left out on purpose

Recorded as decided, so the README section is a record and not hindsight. The reasoning for
each is in `DECISIONS.md`.

- **The `serviceCard` component** (DummyJSON promo cards). Cut for time. The heterogeneous
  feed requirement needs articles plus one more source, and the weather card is that source,
  reading Open-Meteo for real, so cutting this leaves every must-have intact.
- **Search and filter.** A nice-to-have that needs debounce handling and its own empty-result
  state. Not worth 1.5 hours against the freshness policy.
- **Animations and transitions.** Pure polish. First thing cut.
- **The `movie` component** (Studio Ghibli). Designed, and built only if Wednesday has time
  left after the documents. Chosen over `serviceCard` for the single optional source because
  it is cheaper to build (22 static records, one request, no arithmetic), it looks the most
  different from an article card (a wide banner carousel), and its correct time-to-live is
  "effectively forever", which is the sharpest illustration that the policy is per-source
  rather than one global number.
- **Hand-written pagination.** Paging 3 is used instead. The three reasons I first gave for
  hand-writing it were all false, and a spike proved it. See `DECISIONS.md`.
- **In-feed promotional cards.** Cut with the `serviceCard` component. They would use
  `PagingData.insertSeparators`, whose constraint is recorded in `DECISIONS.md`.
- **An emulator in CI.** View tests run on the JVM through Robolectric, so they are in CI.
  Device behaviour is checked by hand over `adb` and the evidence is written down. An emulator
  would add six to twelve minutes to a two and a half minute run, to make repeatable a handful
  of checks already done once. See `DECISIONS.md`.
- **TMDB.** Needs an API key, and a submission that asks the reviewer to get a secret before it
  runs works against the single-command rule.
- **TVMaze.** Considered for the carousel because its schedule endpoint changes daily. Dropped
  because it serves TV shows, not films, so a component named `movie` holding TV schedule data
  would be a name that lies.
- **Background refresh with WorkManager.** Refresh happens only at moments the user can see.
  Spending the user's mobile data while they are not looking is what the brief warns against.
- **Every layer of every component as a module.** The four layers are the shape, but a layer is
  created only when there is something to put in it. A `domain` module exists when logic has no
  single model to belong to, which means it coordinates more than one repository or source.
- **Typed project accessors and a mocking library.** String project paths and hand-written
  fakes instead.
