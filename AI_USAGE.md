# AI usage

Claude Code (Opus 5) wrote most of the code and most of the words. The README describes the
process. This file is about how I treated that output, which came down to one habit:
**AI-generated reasoning is most useful as a hypothesis, never as a fact.**

Every correction below had the same shape. A claim ran ahead of its evidence, and I had to notice
it and go and check.

## Three things I asked for

I asked these in Chinese and have translated them.

- "Stop arguing and prove it. Write throwaway tests that make Paging 3 do the three things the
  plan says it cannot, then delete them."
- "`articles` has an `api` folder and a `domain` folder. Those are the same thing. Justify the
  split or drop one."
- "I know the films do not change. Assume they will, and give them an ordinary time to live like
  every other source."

The first reversed an architecture decision. The second deleted a module. The third reversed a
decision already argued for in writing, and `DECISIONS.md` carries the reversal next to what it
replaced. None of the three asks for an opinion. Each one states a judgement and gives the
assistant something to go and do.

## What I accepted as it was

The two dependency rules, expressed once in a convention plugin per layer instead of my
per-module plan. Core library desugaring, so `java.time` works on `minSdk 24`. Modelling
`Offline` as a state that can carry content instead of a kind of `Error`. Room as the only thing
the UI reads, so a failed refresh cannot empty the screen.

## What I rejected, and why

**A library's limits.** The plan gave three reasons for hand-writing the pagination, each of the
form "Paging 3 cannot do X". I asked for a spike instead of a fourth reason, and five throwaway
tests answered all three: `initialize()`, `PagingState.anchorPosition` and `PagingData.from` each
do the thing that had been called impossible. Looking for a reason to keep a decision already
made produces "cannot" instead of "how".

**A number nobody had priced.** The policy stretched the refresh allowance four times over on a
metered connection, to save the reader's data. I priced it. A page is 17 KB, so the change saves
about 50 KB an hour of reading, and one article thumbnail is 50 KB. The multiplier went.

**A green build.** I asked whether CI was running the view tests. It ran `testDebugUnitTest`,
which does not exist in a plain Kotlin module, so CI had been skipping 25 of 33 tests in silence.
The failure mode here is a passing build, so nothing would have raised it.

## What only the screen could tell me

Two reviews and sixty-seven tests passed over three faults I found by opening the app or by
asking why the code was shaped as it was: an empty module that a decision record still defended, a
rule written as a list that would go stale when the next component arrived, and a requirement
marked done because `onBack` had been passed down, when a normal article had nothing on screen to
tap. None of them is in the code to be read. An empty module only means something next to the
paragraph defending it, and a missing button is an absence.

## What I would keep

1. **A claim about a library's limits gets a spike, not an argument.**
2. **A number in a design gets priced before it is written down.**
3. **A requirement about what the user sees is checked on the screen.** "Is the callback wired"
   and "is there anything to tap" are different questions, and only the first can be answered
   from the code.
