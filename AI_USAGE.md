# AI usage

> **Working draft, deliberately long.** Written while the details were still to hand, to be cut to
> one page before submission. The specifics are here on purpose: they are what would be lost if
> this were written from memory at the end.

## What I used

Claude Code (Opus 5) as the main tool, driven through two things:

- **OpenSpec** for spec-driven changes. Each slice gets a proposal, a delta spec of observable
  behaviour, a design document and a task list, all committed under `openspec/`. The plan is
  reviewable next to the code, and the revisions are in the history.
- **A skill library** (`superpowers`) for brainstorming, plan writing, spikes and code review.

Work lands through pull requests. Review is done by a subagent that is given the diff and the
requirements but **not** the session history, so it cannot inherit the author's blind spots.

My role was to set the constraints, price the claims, and decide. The model wrote most of the
words and most of the code. Almost every significant correction in this project came from me
asking a question rather than from me spotting a bug.

## Representative things I asked

- "Why do some components have no `domain` module?"
- "有沒有 pageIndex 有什麼差別嗎" — what does that column actually change?
- "一次載入20個 問題在第19~20的時候嗎 第21需要等載入?"
- "為什麼用 Unmetered 而不是直接 wifi?"
- "CI view tests unit tests 都有跑了?"
- "pr已經包到paging是刻意的嗎"
- "是不是使用 insertSeparators 就可以穿插 service card"
- Seven objections to a finished-looking plan, listed flatly with no explanation. All seven landed.

## What I accepted as it was

- The two dependency rules — only `:app` may depend on a `data` module, and a component sees
  another only through its `api`. Expressing them once in a convention plugin per layer was better
  than my per-module plan.
- Core library desugaring so `java.time` works on `minSdk 24`. I had not connected that build
  setting to how readable the freshness tests would be.
- Modelling `Offline` as a state that can carry content rather than a kind of `Error`.
- Room as the only thing the UI reads, so a failed refresh cannot empty the screen.

## The pattern: claims running ahead of evidence

This is the one thing I would want a reader to take from this file. Every correction below has
the same shape.

### 1. Three false claims about Paging 3, killed by a thirty-minute spike

The plan hand-wrote the pagination and justified it three ways. All three were wrong:

| The claim | What is actually true |
|---|---|
| `RemoteMediator` takes the load decision away, so the freshness policy cannot be tested | `initialize()` exists for exactly that decision, and a `RemoteMediator` is an ordinary class a test can drive |
| Refresh invalidates and restarts, so the reader loses their place | The refresh branch need not delete. Paging reloads around `PagingState.anchorPosition` |
| `PagingData` is a diff stream, so a JVM view test cannot drive a paged screen | `PagingData.from(items, sourceLoadStates = ...)` exists for tests and previews |

I asked for a spike instead of another argument. Five throwaway tests: `room-paging` under KSP on
Kotlin 2.3 with AGP 9, the policy in `initialize()`, a `PagingData` flow rendering under
Robolectric, and the four content states coming out of a plain function. All passed, then deleted,
and the architecture changed.

Every one of the three was of the form "the library cannot do X" when the truth was "the library
does X differently". Looking for a reason to keep a decision already made produces "cannot"
rather than "how".

### 2. A number that saved one thumbnail an hour

The policy stretched the refresh allowance four times on a metered connection, justified as saving
the reader's data. Priced: a page is 17 KB, so ten minutes to forty saves about 50 KB an hour of
reading. **One article thumbnail is 50 KB.**

Rewritten as saving battery, and that had a worse problem — not magnitude but logic. An unmetered
connection also wakes a radio. Metering is about money. Keying a power measure off a money signal
is using one as a proxy for the other, which is the mistake the same design rejects when it insists
the enum is named `Unmetered` and not `WiFi`.

The multiplier was removed. The connection now governs pictures and look-ahead, where the bytes
actually are.

### 3. Four hours for seven and a half hours of work

Forty-three tasks in four hours is five and a half minutes each, against tasks like "Retrofit
service plus response types plus a decoding test". Worse, the plan was horizontal — all of the
policy, then all of the storage, then all of the network — so running out of time before the last
group would have left an app still showing placeholders.

Rewritten as five vertical passes, each ending with the app working and better than before, with
the estimate stated rather than smoothed and a written cut order.

### 4. A gap dismissed with the wrong frequency

Refresh fetched only the newest page, and the gap that leaves was written into the README as a
limitation, justified by "a feed refreshed every ten minutes does not need more machinery". But
ten minutes only applies while the app is in use. The source publishes twenty to forty articles a
day, so a reader who opens the app daily is **more than a page behind every time**. The gap was the
normal case, not an edge case, and the reader would have scrolled from today into yesterday with a
day missing and nothing said. It is now implemented: refresh pages forward until it meets an
article already stored, capped at five pages.

### 5. A green build that ran two thirds of the tests

I asked whether CI was running the view tests and the unit tests. Rather than answering, we listed
the task graph. CI ran `testDebugUnitTest`, which does not exist in a plain Kotlin module, so every
JVM module was skipped silently: **25 of 33 tests**, green the whole time. That covered the feed
composition rules and the fake clock, and the next slice puts the freshness policy in a JVM module
too, so its entire suite would have been skipped.

The failure mode is a green build, so nothing would have flagged it.

### 6. A pull request that contradicted its own body

The body said "Planning only. No production code changes". The diff changed the version catalog and
two convention plugins, adding dependencies nothing used yet to the classpath of every `data` and
`ui` module. The body is written after the work, by which time what is in the branch is no longer
fresh.

The fix is mechanical rather than attitudinal, and is now in `AGENTS.md`: **read
`git diff --stat` before writing a pull request body.**

### 7. Two verification commands that reported success by failing

- `grep androidx` was used to check "no Android in an `api` module". `paging-common` resolves to a
  plain JVM variant that pulls `androidx.annotation-jvm`, so the check now reports a violation that
  is not one. The real guarantee was always that `todayfeed.jvm` never applies the Android plugin,
  so an `android.*` import does not compile.
- Worse: a grep over a Gradle task's output reports zero when **the task itself failed**. I claimed
  "0 Android artifacts" from a command that had errored. The answer "the command broke" was wearing
  the answer "none".

### 8. A screenshot of a screen the app had already left

I installed the app on a phone to watch the star turn on and off. I took a screenshot, read the
star's position off it, tapped there twice, and got back a filled star. I was one sentence from
calling that proof.

The screenshot was of the article screen, and by the time the taps landed the app had gone back to
the feed. I still cannot say what those two taps hit. I then explained the filled star as one the
reader had saved in an earlier test, which was a second claim with nothing behind it. To settle
that I copied the database off the phone, and the copy came back on the old schema with its write
log not applied, so it had no `savedAt` column to read. A third tap, meant for the Saved tab, hit
the navigation bar and left the app.

Tapping the right place did show the real thing: outline, filled, outline, with the other rows
untouched. Three wrong taps is the small part. The part worth keeping is that a screenshot is the
most convincing evidence in this file, and my explanation of the first wrong one was a guess
wearing the clothes of a finding.

### And two more the reviewer found

A subagent review of the first implementation pull request found a comment that justified a bug.
The detail screen used `else` for its last two `ContentState` cases, so `Offline` carrying a cached
article rendered "That article could not be found" while holding the article — and the comment
above it asserted that an article is either saved or never existed, which is false because the feed
cache holds articles nobody saved. A wrong comment that explains why wrong code is correct is worse
than either alone: it turns a bug into a documented decision.

The second review found that `ContentState.Loading` was untested on all three screens, and that a
test named "rounds the temperature down" blessed `toInt()`, which truncates toward zero — so
`-3.7` rendered as `-3`, rounded *up*, and Open-Meteo returns negatives.

### And three the reviews did not find

None of these came from a test or from a review. Two came from asking why the code was shaped the
way it is, and one from opening an article.

- **An empty module, defended by sixteen lines of prose.** `:components:articles:domain` held no
  code at all. Three reasons had been written down for it. Choosing Paging 3 moved two of them into
  the remote mediator, which lives in `data`, and the third was a single repository call, which
  coordinates nothing. The decision record went on defending it, so the dead weight sat in the
  document rather than in the build. Three unused dependencies came out with it, and not one of
  them could ever have failed a build.
- **A rule written as a list, which goes stale.** My replacement for that entry said "only `feed`
  has a `domain` module". That is a list. It needs editing the next time a component arrives, which
  is the same failure as the entry it replaced. The rule it became has no list: the four layers are
  the shape, and a layer with nothing to hold is not created.
- **A requirement marked done, verified by reading the code.** The spec asks for a way out of the
  detail screen inside the app. `onBack` was passed down, and it was used — on the three states
  that go wrong. On a normal article there was nothing to tap. Every code-shaped check passed, and
  so did 67 unit tests, because none of them asked what was on the screen.

What the three have in common is that none of them is visible in the code. An empty module only
means something next to the paragraph defending it, a rule goes stale in the future, and a missing
button is an absence. The assistant is good at reading what is there.

## What I made it verify instead of recall

Model knowledge has a cutoff, so anything version-shaped had to be checked. Four times, and three
went badly:

| Question | Answer |
|---|---|
| Newest Kotlin? | **2.4.10 is unusable.** KSP has no 2.4 release and both Room and Hilt need it, so 2.3.21 |
| Newest AGP? | **9.3.1 is unusable.** Android Studio 2025.3 opens up to 9.2.1, and the reviewer's IDE is the real ceiling |
| Robolectric on `compileSdk` 37? | **No jar for 37.** The emulated level is pinned to 36 in one properties file |
| Paging 3 on this stack? | **Works.** The one that was checked and came back clean |

The general rule that came out of it: the ceiling on "newest usable" is not the artifact repository,
it is whichever tool consumes it — the annotation processor, the IDE, the test framework. Each has
its own lag.

The same habit was applied to the sources. All four candidate APIs were called before being written
into the plan, including confirming which answer `304 Not Modified` with an empty body. That is how
we found that the two **static** sources are the cheap ones to revalidate and the fastest-changing
source is the expensive one — the opposite of the intuition the design had been built on. The
response fixture in the tests is a real saved response, which is how we found the source sends `id`
as a number while the model carries it as text.

## What I would keep from this

Three practices, each with a specific trigger rather than a good intention:

1. **A claim about a library's limits gets a spike, not an argument.** Thirty minutes changed an
   architecture decision that three paragraphs of reasoning had got wrong.
2. **A number in a design gets priced before it is written down.** Both bad justifications for the
   metered multiplier would have died on contact with 17 KB and 50 KB.
3. **A verification command has to be able to fail.** Check the exit status, and prefer a check
   that a compiler enforces over a grep that approximates it.
4. **A requirement about what the user sees is checked on the screen.** "Is the callback wired" and
   "is there anything to tap" are different questions, and only the first is answerable from the
   code.
