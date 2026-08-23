# AI usage

## What I used

Claude Code (Opus 5) as the main tool, driven through two things:

- **OpenSpec** for spec-driven changes. Each slice of work gets a proposal, a delta spec of
  observable behaviour, a design document and a task list, all committed under
  `openspec/`. The plan is reviewable next to the code.
- **A skill library** (`superpowers`) for brainstorming, plan writing and test-first work.

My role was to set the constraints, challenge the reasoning, and decide. The model wrote
most of the words and most of the code. I chose the architecture, cut the scope when the
deadline moved, and rejected several of its suggestions. Where we disagreed, I asked for
the reasoning rather than the change.

## Things I asked for

- "Design the freshness policy. Here is my draft with a time-to-live per source. Probe it
  before agreeing with it."
- "Why do some components have no `domain` module?"
- "Find a movie source that needs no API key, or record why we are dropping the feature."
- "Cut the plan to fit a Thursday deadline. If it does not fit, say what gets dropped."

## What I accepted as it was

- The two dependency rules: only `:app` may depend on a `data` module, and a component sees
  another component only through its `api`. Expressing them once in a convention plugin per
  layer was a better idea than my per-module plan.
- Core library desugaring so `java.time` works on `minSdk 24`. I had not connected that
  build setting to how readable the freshness tests would be.
- Modelling `Offline` as a state that can carry content rather than a kind of `Error`.

## What I rejected or rewrote

- **TMDB with an optional API key.** The first plan added a build-config mechanism so the
  movie carousel could hide itself when the key was missing. I would rather not ask a
  reviewer for a secret at all, so we changed the source. Every source is now keyless and
  the project ships no configuration mechanism.
- **Dense English.** The first drafts of the planning documents were long sentences with
  clauses stacked up. The reviewers are not native English speakers and communication is
  graded, so I made plain English a rule and had everything rewritten.
- **Component names.** It proposed `shopping`, then `offers`, then `promotions`. I wanted the
  brief's own word, `serviceCard`, so a reviewer can map the repository to the requirements
  with no translation. It also wanted to keep `tvschedule`; I asked for `movie`, and that
  disagreement is what exposed the naming problem below.

## One thing it got wrong that I caught

I asked why three components have no `domain` module. The design document said they "have no
business logic worth a module". That is false, and asking the question is what showed it:
the promo card has discount arithmetic, the carousel has sorting and filtering, the weather
card maps a numeric code to a condition. The real rule is different — **a `domain` module
exists when logic has no single model that can own it**, meaning it coordinates more than
one repository or source. Only two components qualify. The rest keep their logic on their
models in `api`. That version can be applied to a component I have not written yet; the
original could not.

The same thing happened again with the `api` modules. I looked at them and saw only a model
file, so I asked whether that was all `api` was for. It was not: `api` exists for the
repository interface, and the model is only the vocabulary that interface uses. But the code
did not show that, because the placeholder view models had empty constructors and depended on
nothing. So the architecture's central claim was written down in three documents and proved
by nothing. Two questions from me, and both times the answer was "the reasoning is right and
the artefact does not match it".

A second one, nearly missed. It read the AGP 9 release notes and told me the Compose
compiler plugin is no longer needed. The build disagreed immediately. It also proposed
`movie` as the name for a component backed by TVMaze, which serves television schedules and
not films. Rather than accept a name that lies about its contents, we changed the data
source to the Studio Ghibli film API.

## What I made it verify instead of recall

Model knowledge has a cutoff, so anything version-shaped or network-shaped had to be
checked. All four candidate APIs were called before being written into the plan, including
confirming which ones answer `304 Not Modified` with an empty body. Every library version
was resolved from its actual repository. That is how we found that KSP has no release for
Kotlin 2.4, which quietly rules out the newest Kotlin because Room and Hilt both need KSP.

The same habit caught a worse one. I asked whether CI was running the view tests and the unit
tests, and rather than answering, we listed the task graph. CI ran `testDebugUnitTest`, and a
plain Kotlin module has no debug variant, so every JVM module was skipped without a word: 25 of
33 tests. The build had been green the whole time. The next slice puts the freshness policy in
a JVM module, so its entire test suite would have been skipped too.
