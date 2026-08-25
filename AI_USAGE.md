# AI usage

Claude Code (Opus 5) wrote most of the code and most of the words, driven through OpenSpec for
spec-driven changes and a subagent review that never sees the session history. The process is
described in the README. This file is the judgement: what was accepted, what was rejected, and
what that turned up.

## Three things I asked for

Translated; they were asked in Chinese.

- **"Stop arguing and prove it. Write throwaway tests that make Paging 3 do the three things the
  plan says it cannot, then delete them."**
- **"`articles` has an `api` folder and a `domain` folder. Those are the same thing. Justify the
  split or drop one."**
- **"I know the films do not change. Assume they will, and give them an ordinary time to live
  like every other source."**

The first reversed an architecture decision. The second deleted a module. The third reversed a
decision already argued for in writing, and `DECISIONS.md` records the reversal next to what it
replaced.

## What I accepted as it was

The two dependency rules, expressed once in a convention plugin per layer instead of my
per-module plan. Core library desugaring, so `java.time` works on `minSdk 24`. Modelling
`Offline` as a state that can carry content instead of a kind of `Error`. Room as the only thing
the UI reads, so a failed refresh cannot empty the screen.

## What I rejected, and what it showed

Every correction had the same shape: a claim running ahead of its evidence.

**Three false claims about Paging 3, killed by a thirty-minute spike.** The plan hand-wrote the
pagination and gave three reasons, each of the form "Paging 3 cannot do X": the freshness policy
could not be tested, a refresh would lose the reader's place, and a JVM view test could not drive
a paged screen. Five throwaway tests answered all three and were then deleted. `initialize()`,
`PagingState.anchorPosition` and `PagingData.from` each do the thing called impossible. Looking
for a reason to keep a decision already made produces "cannot" instead of "how".

**A number that saved one thumbnail an hour.** The policy stretched the allowance four times on
a metered connection to save the reader's data. A page is 17 KB, so ten minutes to forty saves
about 50 KB an hour. One article thumbnail is 50 KB. The multiplier was removed.

**A green build that ran two thirds of the tests.** CI ran `testDebugUnitTest`, which does not
exist in a plain Kotlin module, so every JVM module was skipped in silence: 25 of 33 tests, green
the whole time. The failure mode is a green build, so nothing would have flagged it.

## What the reviews did not find

The subagent review caught real bugs, including a wrong `else` that made a cached article render
as "could not be found", with a comment above it explaining why that was correct. A wrong comment
defending wrong code is worse than either alone.

Three things were found only by using the app or by asking why the code was shaped as it is: an
empty module that a decision record still defended, a rule written as a list that would go stale
when the next component arrived, and a requirement marked done because `onBack` was passed down,
when a normal article had nothing on screen to tap. Sixty-seven tests and two reviews had passed.
None of the three is in the code to be read. An empty module only means something next to the
paragraph defending it, and a missing button is an absence.

## What I made it verify instead of recall

Anything version-shaped was checked, and three of four checks went badly: no KSP release for
Kotlin 2.4, no Android Studio that opens AGP 9.3, no Robolectric jar for `compileSdk` 37. The
ceiling on "newest usable" is set by whichever tool consumes the artifact, not by the repository
that holds it. All four candidate APIs were called before being written into the plan, which is
how the two static sources turned out to be the cheap ones to revalidate.

## What I would keep

1. **A claim about a library's limits gets a spike, not an argument.**
2. **A number in a design gets priced before it is written down.**
3. **A requirement about what the user sees is checked on the screen.** "Is the callback wired"
   and "is there anything to tap" are different questions, and only the first can be answered
   from the code.
