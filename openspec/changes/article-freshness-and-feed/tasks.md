> **Timebox: about four hours.** The order below is also the priority order. Group 1 is what the
> brief weighs, so it is finished first and is never the thing that gets cut. If time runs out
> during group 6, a feed that shows real articles without pull to refresh is a better place to
> stop than a policy without tests.

## 1. The policy

- [ ] 1.1 Add `Connection`, `RevalidationCost`, `FreshnessPolicy` and `Decision` to
      `:core:freshness`. Verify: `./gradlew :core:freshness:dependencies` still shows no
      Android, no Room and no Retrofit
- [ ] 1.2 Write the failing tests for `decide` first, as a table over age, time-to-live,
      connection and revalidation cost, using the existing `FakeClock`. Verify: they fail for
      the right reason, which is that `decide` does not exist yet
- [ ] 1.3 Implement `decide` in the order from design.md: nothing stored and offline, nothing
      stored, stored and offline, stored and young enough, stored and past its allowance.
      Verify: the table passes
- [ ] 1.4 Add the allowance multiplier and its cases. Verify: a test shows content that would
      be refreshed on an unmetered connection is served from cache on a metered one, and that
      content far past even the metered allowance is refreshed
- [ ] 1.5 Make a stated server maximum age replace the app's own figure. Verify: a test with a
      ten minute server age and a fifteen minute policy refreshes at ten
- [ ] 1.6 Check the boundary. Verify: tests for exactly at the allowance, one second before and
      one second after, so an off-by-one cannot hide

## 2. Connectivity, and the one value the UI branches on

- [ ] 2.1 Add `Connection` and the `Connectivity` interface to `:core:freshness`, with a KDoc
      line saying the enum answers what a byte costs right now. Verify: the module still has no
      Android on its classpath
- [ ] 2.2 Add `FakeConnectivity` to `:core:testing`, settable from a test. Verify: a test moves
      it from unmetered to offline and the flow emits both
- [ ] 2.3 Implement it in `:core:network` over `NetworkCapabilities`, reading
      `NET_CAPABILITY_NOT_METERED` rather than the transport type. Verify by hand on a device:
      wifi reports unmetered, mobile data reports metered, aeroplane mode reports offline
- [ ] 2.4 Add `DataSaver` and `LocalDataSaver` to `:core:designsystem`. Verify:
      `./gradlew :core:designsystem:dependencies` shows no dependency on `:core:freshness`,
      because the mapping belongs to `:app`
- [ ] 2.5 Map `Connection` to `DataSaver` in `:app` and provide it at the root. Verify: a view
      test reads the local and gets the value the provider was given

## 3. Storage

- [ ] 3.1 Add the `articles` table keyed on the article id, with no page index, and a DAO whose
      article flow is ordered by published time descending. Verify: a Room test writes the same
      article twice with different content and reads back one row with the newer content
- [ ] 3.2 Add the single-row `feed_metadata` table holding the last refresh time, the server's
      stated maximum age, the next offset and whether more exist. Verify: a test writes it twice
      and reads back one row
- [ ] 3.3 Use the `Instant` converter from `:core:database` and add the database class. Verify: a
      test stores and reads an `Instant` and a `Duration` unchanged
- [ ] 3.4 Add the DAO calls the repository needs: upsert a list of articles, read the metadata,
      and count how many of a given list of ids are already stored. Verify: a test shows the
      count is what the gap warning needs

## 4. Network

- [ ] 4.1 Add the Retrofit service for `/v4/articles/` with limit and offset, and the response
      types for `kotlinx.serialization`. Verify: a test decodes a saved copy of a real response
      body, taken from the actual API rather than written by hand
- [ ] 4.2 Read `Cache-Control: max-age` off the response. Verify: a test parses a real header
      value, and a response with no such header yields no stated age
- [ ] 4.3 Map the response to the `Article` model in `api`, including the published time.
      Verify: a test shows the mapped article carries the site name and a parsed timestamp

## 5. The repository

- [ ] 5.1 Replace `InMemoryArticleRepository` with the real one, still implementing the
      `ArticleRepository` interface unchanged, reading from Room only. Verify: `assembleDebug`
      passes with no change to `:components:articles:ui` or `:components:feed:ui`
- [ ] 5.2 Add `FeedState` to `:components:articles:api` and expose it as a flow. Verify: a test
      with fakes shows the fields change independently
- [ ] 5.3 Wire the policy into the refresh path. Verify: a test with a fake clock shows that a
      second open within the allowance makes no call to the fake source, and one past it does
- [ ] 5.4 Implement serve-then-replace. Verify: a test shows the stored articles are emitted
      before the refresh finishes, and the refreshed ones after
- [ ] 5.5 Implement the offline path. Verify: a test with the fake connectivity offline emits
      the stored articles with `mayBeStale` set, and never calls the source
- [ ] 5.6 Implement refresh as an upsert of offset zero that leaves later articles alone.
      Verify: a test loads three pages, refreshes with two new articles at the front, and shows
      all three pages still present with the two new ones at the top
- [ ] 5.7 Implement `loadMore`, the offset and the end of the feed. Verify: tests show the offset
      advances, that re-fetching articles already held after an offset drift adds no duplicate
      row, and that once the source reports no more, further calls make no request
- [ ] 5.8 Warn when a refresh may have left a gap: every article in the refreshed page being new
      to us. Verify: a test with a fully new page zero reports the warning and one with a single
      familiar article does not
- [ ] 5.9 Serialise refresh and load-more behind one mutex. Verify: a test starts a load-more and
      a refresh together and shows the requests do not interleave
- [ ] 5.10 Make a failure leave storage untouched. Verify: a test with a failing source shows the
      stored articles unchanged and `lastFailure` set
- [ ] 5.11 Bind the real repository in `:app` and delete the in-memory one. Verify: only `:app`
      references a data module, checked with the grep in `AGENTS.md`

## 6. The screens

- [ ] 6.1 Add `FeedUiState` and map `FeedState` onto it in the feed view model, following the
      table in design.md. Verify: view tests cover each row of that table
- [ ] 6.2 Show the four content states for real instead of the fixed list. Verify: the existing
      view tests still pass unchanged, since they test the stateless screen
- [ ] 6.3 Add Coil to the version catalog and an `ArticleImage` composable to
      `:core:designsystem` that reads `LocalDataSaver` and shows the text-only placeholder while
      loading or on failure. Verify: a view test with no picture and one with an unreachable URL
      both still show the article's title
- [ ] 6.4 Put the thumbnail on the article card and a wide picture at the top of the detail
      screen. Verify: previews in both themes, and by hand that a card with no picture keeps its
      layout
- [ ] 6.5 Add the out-of-date marker when offline with content. Verify: a view test shows the
      marker with content present and offline, and no marker when online
- [ ] 6.6 Add pull to refresh, and show it running. Verify by hand: pulling refreshes even
      straight after a refresh; a view test shows the indicator follows `isRefreshing`
- [ ] 6.7 Start the next page a few items before the end, with the distance taken from
      `LocalDataSaver`, and stop at the end of the feed. Verify: a view test shows the trigger
      fires earlier with the saver off than on, and that reaching the end with `hasMore` false
      calls nothing
- [ ] 6.8 Offer a way to retry a failed page without losing what is loaded. Verify: a view test
      shows the loaded articles stay and the retry calls back
- [ ] 6.9 Refresh when the app comes to the foreground and when the tab is shown. Verify by hand
      on a device: leaving the app for longer than the allowance and returning refreshes
- [ ] 6.10 Make the detail screen read from storage. Verify: a view test shows an article renders
      from the fake repository, and by hand that detail opens with aeroplane mode on

## 7. Acceptance

- [ ] 7.1 Write the freshness section of the README: what fresh means here, the allowance for
      each source and where each number comes from, why refresh only happens where the reader
      can see it, and the three things a metered connection changes. Say plainly that the
      allowance is about radio wakeups and the image policy is about data, with the sizes that
      make that true. Verify: a reader who has not seen the code can say when the app will and
      will not call the network, and why the two levers exist
- [ ] 7.2 Add to the README limitations: a refresh can leave a gap when more than a page was
      published since the last one, and the warning for it is conservative. Verify: the wording
      says what a reader would actually notice
- [ ] 7.3 Check every scenario in the `article-feed` spec, noting which are covered by a test
      and which by hand. Verify: each scenario has one or the other against it, and anything
      neither is written into the README limitations
- [ ] 7.4 Run the whole thing from a fresh clone with aeroplane mode toggled part way. Verify:
      `./gradlew assembleDebug detekt ktlintCheck test` passes, the feed loads, aeroplane mode
      keeps the articles with a marker, and turning it off clears the marker
