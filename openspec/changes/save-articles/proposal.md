## Why

Saving is the last must-have in the assignment that has no code behind it. The Saved tab is
already on screen and always empty, because `ArticleRepository.observeSavedArticles()` returns an
empty flow. A reader can open an article but cannot keep it, which is the one thing the brief
asks for that the app cannot do.

## What Changes

- A reader can save an article and unsave it again, from the feed and from the article screen.
- The Saved tab shows what they saved, newest saved first, and its empty state says how to add
  something rather than reporting a failure.
- Saved articles are readable with no network, and never expire. The freshness policy decides
  when to refresh the feed; it has no say over what the reader chose to keep.
- The save control shows the current state, so the reader can tell a saved article from an
  unsaved one without opening it.

Not in this change, on purpose: no separate sync, no export, no folders or tags, and no change to
how the feed itself is fetched or refreshed.

## Capabilities

### New Capabilities

- `saved-articles`: keeping an article, letting it go again, and reading what has been kept with
  no network.

### Modified Capabilities

None. The feed's requirements do not change: this adds a second reason for a row to be in the
article table, and takes nothing away from the first.

## Impact

- `:components:articles:api` gains save and unsave on the repository contract.
- `:components:articles:data` stores the fact, which is a Room schema change and therefore a
  migration.
- `:components:articles:ui` gains the control on the card and on the article screen, and the
  Saved screen stops being a placeholder.
- `:components:feed:ui` draws the article card, so the control appears in the feed through it.
- No new dependency, no new module, and no change to the two architecture rules.
