## Purpose

The feed is meant to mix kinds of content, not styles of one kind. A row of films that scrolls
sideways is the clearest way to show that, and it is also the source whose age behaves least like
the others, which is what makes the freshness policy legible.

## ADDED Requirements

### Requirement: The feed shows a row of films

The app SHALL show films in the feed as a row that scrolls sideways, between the weather card and
the articles. Each film SHALL show its title, the year it came out, its review score where the
source gives one, and its picture where the source provides one. A film whose picture is missing
or cannot be loaded SHALL still show its text.

The row SHALL be ordered by review score, best first. A film with no score SHALL come last rather
than be treated as scoring nothing, and films sharing a score SHALL keep a stable order.

#### Scenario: Films are shown

- **WHEN** the feed is opened and films have been fetched
- **THEN** a row of films is shown between the weather card and the articles
- **AND** each film shows its title, its year and its score

#### Scenario: The best film is first

- **WHEN** the row is drawn
- **THEN** the film with the highest score is at the start of the row
- **AND** a film the source gives no score for is at the end

#### Scenario: The row scrolls on its own

- **WHEN** the reader scrolls the row of films sideways
- **THEN** the feed does not scroll up or down
- **AND** the position of the row is kept while the feed is scrolled

#### Scenario: A film with no picture

- **WHEN** a film has no picture, or its picture cannot be loaded
- **THEN** its title and year are still shown

### Requirement: Each band of the feed is named

The feed SHALL name each band of content it shows, so that one kind of content cannot be taken
for another. A band with nothing in it SHALL NOT be named.

#### Scenario: The bands are named

- **WHEN** the feed shows films and articles
- **THEN** the row of films is under a name of its own
- **AND** the articles are under a name of their own

#### Scenario: An empty band is not named

- **WHEN** there are no articles to show
- **THEN** no name for the articles is shown

### Requirement: The films never take the feed away

The films SHALL never replace, hide or delay the articles. A failure to fetch them SHALL leave the
rest of the feed as it was, and SHALL NOT show an error.

#### Scenario: The films cannot be fetched

- **WHEN** the film source cannot be reached
- **THEN** the weather card and the articles are shown as usual
- **AND** no row of films is shown, and no error is shown in its place

#### Scenario: Offline with nothing fetched

- **WHEN** the app is opened with no network and no films have been fetched
- **THEN** the feed is shown without the row
- **AND** the out-of-date marker is shown as it would be anyway

### Requirement: Films are asked for on the same terms as everything else

The app SHALL apply the same freshness decision to films as to the other sources. Films already
held SHALL be shown without asking again while they are inside their allowance, and the source
SHALL NOT be asked for them when there is no connection.

#### Scenario: Inside the allowance

- **WHEN** the feed is opened again inside the films' allowance
- **THEN** the films already held are shown
- **AND** the film source is not asked

#### Scenario: No connection

- **WHEN** a refresh happens with no connection
- **THEN** the film source is not asked
- **AND** whatever films are held are still shown
