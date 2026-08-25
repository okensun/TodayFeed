# Tasks

The one optional feature, built after the submission documents were finished. Estimated at about
three hours, most of it in the two feed modules rather than in the new component.

If time runs out, the cut order is section 4, then section 3. Sections 1 and 2 leave the app
exactly as it was, so stopping after either of them costs the reader nothing.

## 1. The component — about fifty minutes

- [x] 1.1 Create `components/movie/{api,data,ui}` with their build files and namespaces, and
      register them in `settings.gradle.kts`. Verify: `./gradlew assembleDebug` builds, and
      `./gradlew :components:movie:api:dependencies` shows no Android artifact
- [x] 1.2 Add `Film` to `movie/api/models` and `FilmRepository` to `movie/api`. Verify: the
      interface names `Film` and nothing else, and the module is plain Kotlin
- [x] 1.3 Add the Retrofit service, its response type and the mapping in `movie/data`. Verify: a
      test decodes a real saved response and checks the title, the year and the banner
- [x] 1.4 Add `DefaultFilmRepository` with the freshness decision and a twelve hour
      allowance. Verify: tests show a second ask inside the allowance makes no request, one past
      it does, and no connection makes none

## 2. The card — about forty minutes

- [x] 2.1 Add `FilmCarouselCard` to `movie/ui`: a `LazyRow` of films, each with its banner, title
      and year, and its own list state. Verify: a view test shows two films and that a film with
      no picture still shows its title
- [x] 2.2 Check the two scroll directions by hand: a sideways drag moves the row and not the
      feed, a vertical drag moves the feed and not the row. Done on an emulator: dragging the row
      moved it from Only Yesterday to My Neighbors the Yamadas while the weather card stayed put

- [x] 2.3 Show the review score on the card and order the row by it, best first. Verify: tests
      show the best score first, no score last, and a tie broken by title

## 3. Into the feed — about fifty minutes

- [x] 3.1 Add `FeedSection.Films` and take the film repository in `ObserveFeedSections`, ordered
      weather, films, articles. Verify: tests cover both sources present, each missing on its own,
      and both missing
- [x] 3.2 Draw the new section in `feed:ui` and add the film repository to `RefreshSections`.
      Verify: a view test shows the row between the weather card and the first article
- [x] 3.3 Bind the repository from `:app`. Verify by hand on a device that the row appears with
      the films in it
- [x] 3.4 Name each band of the feed. Verify: a
      view test shows the articles heading above the first article, and none when there are no
      articles. Checked on an emulator: the row reads 100, 100, 97, 97, 96, 95 from the left

## 4. Saying so — about twenty minutes

- [x] 4.1 Move the component out of the README's cut list and into what is in the app, and add
      the film allowance to the freshness section as the third answer. Verify: the section names
      three sources and where each number comes from
- [x] 4.2 Record in `DECISIONS.md` that the films get an ordinary allowance rather than being
      treated as permanent, and why. Verify: the entry says what was turned down
