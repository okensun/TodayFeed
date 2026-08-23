> **Estimate: about seven and a half hours, against a four hour block.** That is stated rather
> than hidden, because the first draft of this list claimed four hours for the same work and the
> arithmetic never supported it. Forty-three tasks in four hours is five and a half minutes each,
> and these are not five minute tasks.
>
> The list is therefore five passes, not five layers. Every pass ends with the app working and
> better than it was, so an estimate that is wrong by half still leaves something delivered. The
> earlier version was horizontal — all of the policy, then all of the storage, then all of the
> network — and with that shape, running out of time in the fourth group leaves an app still
> showing placeholders.
>
> **Cut order if time runs out: pass 5, then pass 4.** Nothing before them is optional, because
> passes 1 to 3 are the must-haves and the correctness of what they ship. The README write-up in
> 3.6 is never cut; it is written where it sits so that it exists even if 4 and 5 do not.

## 1. Real articles on the screen — about two hours

The thinnest thing that is worth having: one page, from the network, through Room, on screen.
The policy exists but only knows one question.

- [ ] 1.1 Add `Connection`, `Decision` and `decide` to `:core:freshness`, with the five cases
      from design.md. Verify: `./gradlew :core:freshness:dependencies` shows no Android, no Room
      and no Retrofit
- [ ] 1.2 Write the truth table for `decide` first, using the existing `FakeClock`: nothing
      stored, nothing stored and offline, stored and offline, within the allowance, past it.
      Verify: they fail because `decide` is empty, not because they are wrong
- [ ] 1.3 Implement `decide` in the order from design.md. Verify: the table passes, and a test
      shows a stated server maximum age replaces our figure
- [ ] 1.4 Check the boundary. Verify: tests at exactly the allowance, one second before and one
      second after, so an off-by-one cannot hide
- [ ] 1.5 Add `Connectivity` to `:core:freshness` and `FakeConnectivity` to `:core:testing`.
      Verify: a test moves the fake from unmetered to offline and the flow emits both
- [ ] 1.6 Add the `articles` table keyed on the article id, the single-row `feed_metadata` table,
      the DAO and the database. Verify: a Room test writes the same article twice with different
      content and reads back one row holding the newer content
- [ ] 1.7 Add the Retrofit service for `/v4/articles/` and its response types. Verify: a test
      decodes a saved copy of a real response body, taken from the API rather than written by
      hand
- [ ] 1.8 Read `Cache-Control: max-age` off the response. Verify: a test parses a real header
      value, and a response without the header yields no stated age
- [ ] 1.9 Write the real `ArticleRepository`: fetch page zero when the policy says to, upsert,
      expose the articles from Room. Verify: a test with a counting fake source shows a second
      call inside the allowance makes **zero** requests
- [ ] 1.10 Bind it in `:app`, delete the in-memory one, and drop the placeholder state from the
      feed view model. Verify: `assembleDebug` passes, only `:app` names a data module, and the
      app shows real articles on a device

## 2. Pagination, done correctly — about one and three quarter hours

Including the refresh loop, because paging forward until we meet known content is part of getting
pagination right rather than a later refinement. A reader who opens the app once a day is more
than a page behind every time.

- [ ] 2.1 Add `FeedState` to `:components:articles:api` and expose it as a flow. Verify: a test
      shows the fields change independently
- [ ] 2.2 Add `FeedUiState` to `:components:feed:ui` and map `FeedState` onto it per the table in
      design.md. Verify: a view test covers each row of that table
- [ ] 2.3 Implement `loadMore`, the offset and the end of the feed. Verify: tests show the offset
      advances, that re-fetching articles already held after a drift adds no duplicate row, and
      that once the source reports no more, further calls make **zero** requests
- [ ] 2.4 Make refresh page forward until a page contains an article already stored, capped at
      five pages. Verify: a test with a source three days ahead makes at most five requests and
      leaves no gap; a test where page zero contains something familiar makes exactly one
- [ ] 2.5 Make refresh keep the pages already loaded. Verify: a test loads three pages, refreshes
      with two new articles at the front, and shows all three pages still present with the two
      new ones on top
- [ ] 2.6 Serialise refresh and load-more behind one mutex. Verify: a test starts both together
      and shows the requests do not interleave
- [ ] 2.7 Start the next page a few articles before the end, with an indicator, and stop at the
      end of the feed. Verify: a view test shows the indicator follows `isAppending`, and that
      reaching the end with `hasMore` false calls nothing
- [ ] 2.8 Add pull to refresh. Verify by hand that pulling refreshes straight after a refresh; a
      view test shows the indicator follows `isRefreshing`
- [ ] 2.9 Offer a retry for a failed page without losing what is loaded. Verify: a view test
      shows the loaded articles stay and the retry calls back

## 3. Offline, staleness, and the write-up — about one and a quarter hours

- [ ] 3.1 Implement the offline path in the repository. Verify: a test with the fake connectivity
      offline emits the stored articles with `mayBeStale` set and makes **zero** requests
- [ ] 3.2 Make a failure leave storage untouched. Verify: a test with a failing source shows the
      stored articles unchanged and `lastFailure` set
- [ ] 3.3 Show the out-of-date marker when offline with content. Verify: a view test shows the
      marker with content present and offline, and none when online
- [ ] 3.4 Drop the marker when the network returns, from `Connectivity.observe()`. Verify: a test
      moves the fake back to unmetered and shows the marker clears
- [ ] 3.5 Make the detail screen read from storage. Verify: a view test renders an article from
      the fake repository, and by hand that detail opens with aeroplane mode on
- [ ] 3.6 Write the freshness section of the README, describing what exists at this point: what
      fresh means here, where the ten minutes comes from, why refresh only happens where the
      reader can see it, and the measured request counts. **This task is never cut.** Verify: a
      reader who has not seen the code can say when the app will and will not call the network
- [ ] 3.7 Add to the README limitations: the refresh cap of five pages, and what a reader who has
      been away longer than that will see. Verify: the wording says what they would notice

## 4. Pictures — about forty five minutes

- [ ] 4.1 Add Coil to the version catalog and an `ArticleImage` composable to
      `:core:designsystem` that falls back to the text-only layout while loading and on failure.
      Verify: a view test with no picture and one with an unreachable URL both still show the
      article's title
- [ ] 4.2 Put the thumbnail on the article card and a wide picture at the top of the detail
      screen. Verify: previews in both themes, and by hand that a card without a picture keeps
      its layout
- [ ] 4.3 Check a slow picture does not block the card. Verify by hand with a throttled
      connection: the title and source appear immediately and the picture arrives later

## 5. The metered behaviour, and the numbers — about one and a quarter hours

This is the pass that turns "we do not waste your data" from a claim into an assertion. It is
also the first thing cut, because passes 1 to 3 are the must-haves and this is not one.

- [ ] 5.1 Implement `Connectivity` in `:core:network` over `NetworkCapabilities`, reading
      `NET_CAPABILITY_NOT_METERED` rather than the transport type. Verify by hand on a device:
      wifi reports unmetered, mobile data reports metered, aeroplane mode reports offline
- [ ] 5.2 Add `DataSaver` and `LocalDataSaver` to `:core:designsystem`. Verify:
      `./gradlew :core:designsystem:dependencies` shows no dependency on `:core:freshness`,
      because the mapping belongs to `:app`
- [ ] 5.3 Map `Connection` to `DataSaver` in `:app` and provide it at the root. Verify: a view
      test reads the local and gets what the provider was given
- [ ] 5.4 Make `ArticleImage` stop looking ahead when the saver is on, and cap it at two requests
      at a time. Verify: a view test with the saver on shows pictures requested only for cards on
      screen
- [ ] 5.5 Take the load-more trigger distance from the saver. Verify: a view test shows the
      trigger fires earlier with the saver off than on
- [ ] 5.6 Add the counting fake's byte reporting and assert the table in design.md. Verify: every
      row of that table is a passing test
- [ ] 5.7 Amend the README's freshness section with what a metered connection changes, and say
      plainly that the allowance is not connection-dependent and why the multiplier that used to
      be there was removed. Verify: the section contains no claim without a number behind it

## 6. Acceptance — about thirty minutes

- [ ] 6.1 Check every scenario in the `article-feed` spec, noting which is covered by a test and
      which by hand. Verify: each has one or the other against it, and anything with neither is
      written into the README limitations
- [ ] 6.2 Run it from a fresh clone with aeroplane mode toggled part way. Verify:
      `./gradlew assembleDebug detekt ktlintCheck test` passes, the feed loads, aeroplane mode
      keeps the articles with a marker, and turning it off clears the marker
