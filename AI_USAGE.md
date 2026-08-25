# AI usage

Claude Code (Opus 5) wrote most of the code and most of the words, driven through OpenSpec for
spec-driven changes and a subagent review that never sees the session history. The process is
described in the README. This file is the judgement: what was accepted, what was rejected, and
what that turned up.

## What I accepted as it was

The two dependency rules, expressed once in a convention plugin per layer instead of my
per-module plan. Core library desugaring, so `java.time` works on `minSdk 24` and the freshness
tests read as sentences. Modelling `Offline` as a state that can carry content rather than a kind
of `Error`. Room as the only thing the UI reads, so a failed refresh cannot empty the screen.

## The pattern: claims running ahead of evidence

This is the one thing I would want a reader to take from this file. Every correction had the same
shape, and three are enough to show it.

### 1. Three false claims about Paging 3, killed by a thirty-minute spike

The plan hand-wrote the pagination and justified it three ways. All three were wrong:

| The claim | What is actually true |
|---|---|
| `RemoteMediator` takes the load decision away, so the freshness policy cannot be tested | `initialize()` exists for exactly that decision, and a `RemoteMediator` is an ordinary class a test can drive |
| Refresh invalidates and restarts, so the reader loses their place | The refresh branch need not delete. Paging reloads around `PagingState.anchorPosition` |
| `PagingData` is a diff stream, so a JVM view test cannot drive a paged screen | `PagingData.from(items, sourceLoadStates = ...)` exists for tests and previews |

I asked for a spike instead of another argument. Five throwaway tests, all passing, then deleted,
and the architecture changed. Every one of the three was of the form "the library cannot do X"
when the truth was "the library does X differently". Looking for a reason to keep a decision
already made produces "cannot" rather than "how".

### 2. A number that saved one thumbnail an hour

The policy stretched the refresh allowance four times on a metered connection, to save the
reader's data. Priced: a page is 17 KB, so ten minutes to forty saves about 50 KB an hour of
reading. **One article thumbnail is 50 KB.** Rewritten as saving battery, which was worse — not
in magnitude but in logic, because metering is about money and an unmetered radio also draws
power. The multiplier was removed.

### 3. A green build that ran two thirds of the tests

I asked whether CI ran the view tests and the unit tests. Rather than answering, we listed the
task graph. CI ran `testDebugUnitTest`, which does not exist in a plain Kotlin module, so every
JVM module was skipped silently: **25 of 33 tests**, green the whole time. That covered the feed
composition rules and the fake clock, and the freshness policy was about to land in a JVM module
too. The failure mode is a green build, so nothing would have flagged it.

## What the reviews did not find

The subagent review is good and caught real bugs, including a comment that justified one: a wrong
`else` made a cached article render as "could not be found", and the comment above it explained
why that was correct. A wrong comment defending wrong code is worse than either alone.

But three things were found only by using the app or by asking why the code was shaped as it is:
a module with no code in it that a decision record still defended; a rule I wrote as a list, which
would go stale the next time a component arrived; and a requirement marked done because `onBack`
was passed down, when on a normal article there was nothing on screen to tap. Sixty-seven tests
and two reviews passed.

None of the three is visible in the code. An empty module only means something next to the
paragraph defending it, a rule goes stale in the future, and a missing button is an absence. The
assistant is good at reading what is there.

## What I made it verify instead of recall

Model knowledge has a cutoff, so anything version-shaped was checked. Four times, and three went
badly: Kotlin 2.4 is unusable because KSP has no release for it; AGP 9.3 is unusable because the
reviewer's Android Studio opens up to 9.2; Robolectric has no jar for `compileSdk` 37. The rule
that came out of it is that the ceiling on "newest usable" is not the artifact repository but
whichever tool consumes it, and each has its own lag.

The same habit went to the sources. All four candidate APIs were called before being written into
the plan, which is how we found the two **static** sources are the cheap ones to revalidate and
the fastest-changing one is the expensive one — the opposite of the intuition the design was
built on.

## What I would keep from this

Four practices, each with a trigger rather than a good intention:

1. **A claim about a library's limits gets a spike, not an argument.** Thirty minutes changed an
   architecture decision that three paragraphs of reasoning had got wrong.
2. **A number in a design gets priced before it is written down.** Both justifications for the
   metered multiplier died on contact with 17 KB and 50 KB.
3. **A verification command has to be able to fail.** Check the exit status, and prefer a check a
   compiler enforces over a grep that approximates it.
4. **A requirement about what the user sees is checked on the screen.** "Is the callback wired"
   and "is there anything to tap" are different questions, and only the first is answerable from
   the code.
