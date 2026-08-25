# CLAUDE.md

Read [AGENTS.md](AGENTS.md) first. It has the build commands, the module graph and the
dependency rules. This file only lists the things a linter cannot catch.

detekt and ktlint already enforce line length, indent, star imports, return count and
function count. Do not spend review effort on those.

## Architecture rules that the build enforces, so do not work around them

- Only `:app` may depend on a `data` module. Not even a component's own `ui` or `domain`.
- A component may see another component only through its `api` module. The single
  exception is `:components:feed:ui`, which draws the other components' cards.
- Never write `else` in a `when` over `ContentState`. Write every case out, so the compiler
  stops you when a new one appears. `Offline` carrying cached content must show the content.
- `api` and `domain` modules are plain Kotlin. If you need an Android class there, the
  design is wrong, not the rule.

## Architecture rules the build cannot enforce, so they are easy to get wrong

- A component is `api`, `domain`, `data`, `ui`. That is the shape it takes, not a checklist to
  fill: a layer with nothing to put in it is not created, and a module nothing is taken from is
  not depended on.
- Inside `data`, a file goes by whose data it is. `database/` is ours: entities, DAOs,
  migrations. `source/` is somebody else's: the service and the shapes it answers with. What
  translates between the two stays at the top of `data` — the mapping, the repository, the Hilt
  module. This holds in every component, even where `source/` has one file in it.
- Never call that folder `network`. `:core:network` already means the shared client, the JSON
  and the connectivity reader, and one word for both makes a reader guess.
- Model classes live in a `models` folder in the layer that owns them, such as `api/models`.
- A `ui` module groups by screen, so a screen sits with its view model: `ui/detail`, `ui/saved`.

## Writing

- Plain, general English. A reader below high-school English level must follow it. Short
  sentences, one idea each, common words. This applies to code comments, commit messages,
  pull request bodies and every document.
- Commit body: at most 5 lines. Never list the files; git already has the diff.
- Review comments: inline, on the exact line, two to three lines long.

## How work lands

One branch per OpenSpec change, then a pull request. `gh pr merge --merge`, never a squash.
Review is done by a subagent that does not see the session history, and its findings are
posted as inline comments on the exact line. See AGENTS.md.

## Comments

- At most three lines. Longer than that and nobody reads it; put the reason in `DECISIONS.md`.
- Say what the code means now, or warn about this code's own behaviour.
- Never narrate the change ("previously", "now uses"). Git records history.
- Never restate the code, and never leave commented-out code.
- The best comment is the one a clearer name made unnecessary.

## Kotlin

- `StateFlow` for state that always has a value, not `SharedFlow(replay = 1)`.
- Collect in `repeatOnLifecycle`. Cancel the scope, not child jobs.
- Nothing blocking on the main thread. No expensive work in `init` or in composition.
- Inject dispatchers and `Clock`. Never read the system clock directly.
- Prefer `?.` over `!!`. Catch specific exceptions.

## Tests

- Hand-written fakes, no mocking library. Shared fakes live in `:core:testing`.
- Test names in backticks, read as sentences.
