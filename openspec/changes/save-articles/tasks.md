# Tasks

The last must-have. Estimated at about two hours, and the documents come after it, so the order
below puts the reader-visible behaviour first and the polish last.

If time runs out, the cut order is section 4, then task 3.4. Sections 1 to 3 are the requirement.

## 1. Storing the fact — about forty minutes

- [x] 1.1 Add `savedAt: Instant?` to `ArticleEntity` and bump the database to version 2 with an
      `ALTER TABLE` migration. Verify: a test opens a version 1 database holding an article,
      migrates it, and reads the same article back with a null `savedAt`
- [x] 1.2 Add `setSaved(id, savedAt)` and `observeSaved()` to the DAO, ordered by `savedAt`
      descending. Verify: a test saves two articles in a known order and reads them back most
      recently saved first. A plain `@Upsert` was replaced: it rewrote every column, so one
      refresh would have quietly unsaved everything. There is a test for that
- [x] 1.3 Note on the DAO that anything which ever deletes from `articles` must exclude rows with
      a `savedAt`. Verify: the note is on the query it constrains, not at the top of the file
- [x] 1.4 Add `save(id)`, `unsave(id)` and the real `observeSavedArticles()` to the repository
      contract and its implementation. Verify: a test saves through the repository and sees the
      article arrive in the observed list, and unsaving removes it

## 2. The control on the card and the article screen — about forty minutes

- [x] 2.1 Add `onToggleSave: () -> Unit` to `ArticleRowCard`, with a control that shows which
      state it is in. Verify: view tests show both states and that tapping calls back exactly
      once. `saved` went on the `Article` model rather than the card: Room invalidates the paging
      source on a write, so the feed redraws the card with no extra plumbing
- [x] 2.2 Add the same control to the article screen's bar. Verify: a view test shows the state
      and fires the callback, and the screen still has its way back
- [x] 2.3 Wire both to the repository through their view models. Verify by hand: saving in the
      feed shows in the article screen and in the Saved tab without leaving the screen

## 3. The Saved tab — about thirty minutes

- [x] 3.1 Make the Saved screen show what the repository observes, most recently saved first.
      Verify: a view test with two saved articles shows the newer save above the older one
- [x] 3.2 Give the empty state wording that says how to save something rather than reporting a
      failure. Verify: a view test asserts the wording and that no retry is offered
- [x] 3.3 Unsaving from the Saved tab removes the row without disturbing the rest. Verify: a view
      test unsaves the first of three and shows the other two in the same order
- [x] 3.4 Give `SavedViewModel.onRetry` a body rather than deleting it. Reading storage can
      fail, and the screen has to write the `Error` case out anyway, so a retry that reads again
      is more honest than a branch with nothing behind it. Verify: the retry re-collects, and the
      empty state still offers nothing to retry

## 4. Reading what was saved with no network — about twenty minutes

- [x] 4.1 Confirm the article screen reads a saved article from storage rather than the feed, and
      leave a note saying why. Verify: a test reads an article that is not in any fetched page
- [x] 4.2 Verify by hand on a device: save an article, turn the network off, open the Saved tab
      and read it. Done on a Pixel 6: saved from the feed, the star filled, turned wifi off, and
      the Saved tab still listed it. Nothing was missing to record
- [x] 4.3 Add the saved behaviour to the README's freshness section: what the reader keeps is not
      what the policy decides. Verify: the section says who decides what, in one short paragraph
