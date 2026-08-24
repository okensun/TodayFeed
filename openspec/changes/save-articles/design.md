## Context

The article table already holds every article the feed has fetched, keyed on the article's own
id. Saving adds a second reason for a row to be there. The reader's reason and the cache's reason
are not the same, and the difference is what most of this design is about.

The budget is the other constraint. This is the last must-have, and the documents still have to
be finished after it, so the design picks the cheapest shape that keeps the guarantees.

## Goals

- What the reader saves survives anything the cache does.
- Saving from the feed, the article screen or the Saved tab all mean the same thing, and the
  answer appears in all three at once.
- No new module, no new dependency, no change to the two architecture rules.

## Non-goals

- Sync between devices, export, folders, tags, or a saved count anywhere.
- Any change to how the feed is fetched or when it refreshes.

## Decisions

### Where the fact lives: a column on the article row

**Picked.** `savedAt: Instant?` on the existing `articles` row. Null means not saved. The value
gives the Saved tab its order for free, and "most recently saved first" needs no second field.

**Considered instead.** A separate `saved_articles` table holding a copy of the article. That
makes "saving outlives the cache" structural: an eviction over the articles table could not reach
it even by accident.

**Trade-off.** The separate table is the stronger guarantee and this project usually prefers a
guarantee the compiler or the schema enforces over a rule someone has to remember. It is not
picked here because it duplicates the article in two places, needs its own mapping and its own
tests, and buys protection against an eviction that does not exist: the mediator upserts and
never deletes, which is task 2.2 and has a test. The risk is stated rather than removed, so the
DAO carries a note: anything that ever deletes from `articles` must exclude rows with a
`savedAt`. If eviction is ever added, the separate table becomes the right answer.

### The article screen reads a saved article from storage, not from the feed

An article can be saved and later fall out of what the source returns. Reading is already done
through `findArticle(id)` against Room, so nothing changes here — but it is the reason the spec's
"an article that leaves the feed" scenario holds, and it is worth stating so nobody later
"optimises" the detail screen into reading the paged stream.

### The control lives on the card, and the card is drawn by two components

`ArticleRowCard` is in `:components:articles:ui` and `:components:feed:ui` draws it. Adding the
control to the card therefore puts it in the feed as well, through the one sanctioned
cross-component dependency, with no new dependency and no callback threaded through the feed.

The card takes `saved: Boolean` and `onToggleSave: () -> Unit`. It stays stateless, so the view
test drives it directly and the feed does not have to know what saving means.

### Saving is a suspend call on the repository, not a flow

`save(id)` and `unsave(id)` are one-shot suspend calls. The result is not returned: the screen
already observes the article list, so the change arrives the same way any other change does. That
keeps one path for "what is true now" rather than two that can disagree.

### The schema change is a migration, not a wipe

Adding a column bumps the Room version to 2. `fallbackToDestructiveMigration` would throw away
the reader's saved articles on upgrade, which contradicts the requirement this change is adding.
A one-line `ALTER TABLE` migration costs less than the sentence explaining why the other choice
was acceptable.

## Risks

- **The saved flag rides on the cache row.** Stated above, with the note that guards it. Anyone
  adding eviction must read it.
- **Two screens now write.** The feed and the article screen both toggle. Room serialises the
  write and both screens observe the same table, so the last write wins and both screens show it.
  There is no ordering question to get wrong because the two controls act on the same row.

## Migration plan

Version 1 to 2 adds one nullable column. Nothing needs backfilling: no article was saved before
this change existed, so null for every existing row is the correct starting state.

## Open questions

None.
