> **Estimate: about seven and a quarter hours, against a four hour block.** Stated rather than
> hidden, because the first draft of this list claimed four hours for the same work and the
> arithmetic never supported it.
>
> Paging 3 does not save much of that — maybe half an hour, since the wiring costs some of what
> the machinery saves. What it removes is variance: the paging state machine was the single item
> most likely to overrun and it is now the library's problem.
>
> The list is five passes, not five layers. Every pass ends with the app working and better than
> it was, so an estimate wrong by half still leaves something delivered. **Cut order if time runs
> out: pass 5, then pass 4.** Nothing before them is optional. The README write-up in 3.7 is never
> cut; it sits where it does so that it exists even if 4 and 5 do not.

## 1. Real articles on the screen — about two and a quarter hours

- [x] 1.1 Add `Connection`, `Decision` and `decide` to `:core:freshness`, with the five cases from
      design.md. Verify: `./gradlew :core:freshness:dependencies` shows no Android, no Room, no
      Retrofit and no paging artifact
- [x] 1.2 Write the truth table for `decide` first: nothing stored, nothing stored and offline,
      stored and offline, within the allowance, past it. Verify: they fail because `decide` is
      empty, not because they are wrong.
      No `FakeClock` is involved, which this task originally assumed. `decide` takes `now` as a
      parameter, so its tests need only two `Instant` constants — and `:core:testing` depends on
      `:core:freshness`, so using the fake here would have been circular anyway. The pure
      function paid for itself immediately
- [x] 1.3 Implement `decide` in the order from design.md. Verify: the table passes, and a test
      shows a stated server maximum age replaces our figure
- [x] 1.4 Check the boundary. Verify: tests at exactly the allowance, one second before and one
      second after, so an off-by-one cannot hide
- [x] 1.5 Add `Connectivity` to `:core:freshness` and `FakeConnectivity` to `:core:testing`.
      Verify: a test moves the fake from unmetered to offline and the flow emits both
- [x] 1.6 Add the `articles` table keyed on the article id, the single-row `feed_metadata` table,
      a DAO whose article query returns a `PagingSource`, and the database. Verify: a Room test
      writes the same article twice with different content and reads back one row holding the
      newer content
- [x] 1.7 Add the Retrofit service for `/v4/articles/` and its response types. Verify: a test
      decodes a saved copy of a real response body, taken from the API rather than written by hand
- [x] 1.8 Read `Cache-Control: max-age` off the response and store it with the metadata. Verify: a
      test parses a real header value, and a response without the header yields no stated age
- [x] 1.9 Add the `RemoteMediator` with `decide` in `initialize()` and a `REFRESH` branch that
      fetches one page and upserts. Verify: a test with a counting fake source shows
      `SKIP_INITIAL_REFRESH` inside the allowance and `LAUNCH_INITIAL_REFRESH` past it, with
      **zero** requests in the first case
- [x] 1.10 Build the `Pager` and expose `Flow<PagingData<Article>>` from the repository, mapping
      entities to the `api` model. Verify: a test collects one page of articles
- [x] 1.11 Draw it: the feed screen takes the paging flow, delete the placeholder state, bind in
      `:app` and delete the in-memory repository. Verify: `assembleDebug` passes, only `:app`
      names a data module, and the app shows real articles on a device

## 2. Paging behaviour — about one and a quarter hours

- [x] 2.1 Add the `APPEND` branch and the end-of-pagination signal. Verify: a test shows a second
      page is fetched and that once the source reports no more, further appends make **zero**
      requests. Where the next page starts comes from the stored metadata rather than from
      `PagingState`, because the source counts in offsets and the state describes rows on screen.
      An append also leaves the freshness stamp alone, which has its own test
- [x] 2.2 Make `REFRESH` upsert without deleting, so the reader keeps their place. Verify: a test
      loads three pages, refreshes with two new articles at the front, and shows all three pages
      still present with the two new ones on top. **The code landed in pass 1**, because real
      articles on screen already needed the choice between upsert and delete. Only the test was
      outstanding, and it had to wait for 2.1 to be able to load three pages
- [ ] 2.3 Make `REFRESH` loop until a page contains an article already stored, capped at five
      pages. Verify: a test with a source three days ahead makes at most five requests and leaves
      no gap; a test where page zero contains something familiar makes exactly one
- [x] 2.4 Derive `ContentState` from `loadState.refresh` and `itemCount` in a pure function, with
      `itemCount > 0` first. Verify: unit tests cover every branch, including that an error with
      content already loaded stays `Content`. **Done in pass 1** as `feedContentState`, for the
      same reason 3.5 and 3.6 were pulled forward: a paged feed on screen needs a state to be in.
      Eight tests cover it
- [x] 2.5 Show the refreshing and appending indicators from `LoadState`. Verify: view tests supply
      load states through `PagingData.from` and check each indicator. A refresh with content on
      screen marks it rather than replacing it, so only an empty screen gets the loading state.
      Both indicators carry a content description, which is what a screen reader announces and
      what the tests find them by
- [ ] 2.6 Add pull to refresh, bypassing the policy. Verify by hand that pulling refreshes
      straight after a refresh; a view test shows the indicator follows the refresh load state
- [ ] 2.7 Offer `retry()` for a failed append without losing what is loaded. Verify: a view test
      shows the loaded articles stay and the retry is offered
- [x] 2.8 Set `PagingConfig` with a page size of twenty and a `prefetchDistance` of five. Verify:
      by hand that reaching the end does not stall. **Done in pass 1**; both values are in
      `DefaultArticleRepository`. The test this task asked for was dropped on purpose: asserting
      that a config field equals the constant next to it repeats the code and can catch nothing

## 3. Offline, sections, and the write-up — about one and a quarter hours

- [ ] 3.1 Make the mediator return success without fetching when offline. Verify: a test with the
      fake connectivity offline makes **zero** requests and the stored articles still come through
- [ ] 3.2 Add the offline branch to the state derivation, so content plus offline gives
      `Offline(cached)`. Verify: a unit test covers both offline branches
- [ ] 3.3 Show the out-of-date marker when offline with content. Verify: a view test shows the
      marker with content present and offline, and none when online
- [ ] 3.4 Drop the marker when the network returns, from `Connectivity.observe()`, **and refresh**.
      A review of pass 1 found this is not only cosmetic: `initialize()` runs once per pager, so a
      reader who starts offline with an empty cache never retries without it. Verify: a test
      moves the fake back to unmetered and shows the marker clears
- [x] 3.5 Add `FeedSection` and `ObserveFeedSections` to `:components:feed:domain`, replacing
      `ObserveFeed`. **Pulled into pass 1**: the moment the feed became a paged stream, the old
      combined list stopped compiling, and leaving it out would have removed the weather card from
      the screen — a visible regression in the middle of a slice. Verify: `./gradlew :components:feed:domain:dependencies` shows no paging
      artifact, and tests cover the ordering and the missing-source case
- [x] 3.6 Draw the sections above the paged articles. **Pulled into pass 1** for the same reason. Verify: a view test shows a section and
      articles together, and that no articles with a section present shows the section rather
      than the empty state
- [ ] 3.7 Write the freshness section of the README, describing what exists at this point.
      **Never cut.** Verify: a reader who has not seen the code can say when the app will and will
      not call the network
- [ ] 3.8 Add to the README limitations: the refresh cap of five pages and what a reader away
      longer than that will see. Verify: the wording says what they would notice

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

Turns "we do not waste your data" from a claim into an assertion. First to be cut, because
passes 1 to 3 are the must-haves and this is not one.

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
- [ ] 5.5 Take `prefetchDistance` from the saver: five when off, one when on. Verify: a test shows
      the config differs, and by hand that the list still does not stall on wifi
- [ ] 5.6 Add byte reporting to the counting fake and assert the table in design.md. Verify: every
      row of that table is a passing test
- [ ] 5.7 Amend the README's freshness section with what a metered connection changes, and say
      plainly that the allowance does not depend on the connection and why the multiplier that
      used to be there was removed. Verify: the section contains no claim without a number behind
      it

## 6. Acceptance — about thirty minutes

- [ ] 6.1 Check every scenario in the `article-feed` spec, noting which is covered by a test and
      which by hand. Verify: each has one or the other against it, and anything with neither is
      written into the README limitations
- [ ] 6.2 Run it from a fresh clone with aeroplane mode toggled part way. Verify:
      `./gradlew assembleDebug detekt ktlintCheck test` passes, the feed loads, aeroplane mode
      keeps the articles with a marker, and turning it off clears the marker
