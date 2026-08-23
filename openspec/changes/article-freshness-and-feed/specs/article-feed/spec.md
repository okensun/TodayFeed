## Purpose

The article feed is the list the reader opens the app for. It has to keep working when there is
no network, refresh itself only when that would change what the reader sees, and be honest
about the age of what it shows.

## ADDED Requirements

### Requirement: The feed shows real articles, newest first

The app SHALL show articles taken from the article source, ordered newest first, each with its
title, the name of the site it came from, and its picture where the source provides one. An
article whose picture is missing or cannot be loaded SHALL still show its text.

#### Scenario: First open with a network

- **WHEN** the feed is opened for the first time on a device with a network
- **THEN** the loading state is shown while nothing is stored
- **AND** articles are then shown, newest first

#### Scenario: Nothing to show

- **WHEN** the source returns no articles at all and nothing is stored
- **THEN** the empty state is shown, and not an error

#### Scenario: An article with no picture

- **WHEN** an article arrives without a picture, or its picture cannot be loaded
- **THEN** the article's title and source are still shown

### Requirement: What is stored is what is shown

The app SHALL read the feed only from what it has stored, and SHALL write what it fetches to
storage before showing it. A fetch that fails SHALL leave what is stored untouched.

#### Scenario: A failed refresh does not empty the feed

- **WHEN** articles are already stored and a refresh fails
- **THEN** the stored articles are still shown
- **AND** the reader is told the refresh failed

#### Scenario: Storage survives the app being closed

- **WHEN** the app is closed after articles have been shown, and opened again
- **THEN** the stored articles are shown without waiting for the network

### Requirement: The feed works with no network

The app SHALL show stored articles when there is no network, and SHALL tell the reader that
what they are looking at may be out of date.

#### Scenario: Offline with articles stored

- **WHEN** the feed is opened with no network and articles are stored
- **THEN** the stored articles are shown
- **AND** a marker says the content may be out of date

#### Scenario: Offline with nothing stored

- **WHEN** the feed is opened with no network and nothing is stored
- **THEN** the offline state is shown with a way to try again

#### Scenario: The network comes back

- **WHEN** the reader tries again after the network returns
- **THEN** the feed is refreshed and the out-of-date marker is removed

### Requirement: The feed does not ask again while what it has is young enough

The app SHALL NOT contact the source when what is stored is younger than the age allowed for
that source. Where the source states its own maximum age, that SHALL be used in place of the
app's own figure.

#### Scenario: Opened again straight away

- **WHEN** the feed is opened again a few seconds after a successful refresh
- **THEN** the stored articles are shown
- **AND** the source is not contacted

#### Scenario: The source states its own maximum age

- **WHEN** the source's response states a maximum age different from the app's own figure
- **THEN** the age from the source decides when the next refresh happens

### Requirement: Old content is shown at once and replaced when the refresh arrives

When what is stored is too old but present, the app SHALL show it immediately, refresh behind
it, and replace it when the refresh arrives. The reader SHALL NOT be made to wait on a blank
screen while something is already stored.

#### Scenario: Opened with old content stored

- **WHEN** the feed is opened and what is stored is older than the age allowed
- **THEN** the stored articles are shown immediately
- **AND** the refreshed articles replace them once they arrive

#### Scenario: Opened with nothing stored

- **WHEN** the feed is opened and nothing is stored
- **THEN** the loading state is shown until the first articles arrive

### Requirement: A metered connection is treated as more expensive

The app SHALL tolerate older content before contacting the source when the connection is
metered, and SHALL tolerate it for longer again when the source cannot be checked cheaply.

#### Scenario: Metered connection, content that would be refreshed on wifi

- **WHEN** the feed is opened on a metered connection with content old enough that an
  unmetered connection would refresh it
- **THEN** the stored articles are shown
- **AND** the source is not contacted

#### Scenario: Metered connection, content far past its age

- **WHEN** the content is old enough to exceed even the metered allowance
- **THEN** the feed is refreshed

#### Scenario: Metered connection and pictures

- **WHEN** the feed is scrolled on a metered connection
- **THEN** pictures are fetched only for the articles on screen
- **AND** pictures are not fetched ahead for articles the reader has not reached

#### Scenario: Metered connection and the next page

- **WHEN** the reader approaches the end of the list on a metered connection
- **THEN** the next page is started later than it would be on an unmetered connection

#### Scenario: Refresh a picture already held

- **WHEN** an article's picture has already been fetched and the article is shown again
- **THEN** the picture is not fetched a second time, whatever the connection

### Requirement: Pull to refresh always asks

The app SHALL contact the source when the reader pulls to refresh, whatever the age of what is
stored, and SHALL show that a refresh is running.

#### Scenario: Pull to refresh on fresh content

- **WHEN** the reader pulls to refresh immediately after a successful refresh
- **THEN** the source is contacted
- **AND** a refresh indicator is shown while it runs

#### Scenario: Pull to refresh with no network

- **WHEN** the reader pulls to refresh with no network
- **THEN** the stored articles stay on screen
- **AND** the reader is told the refresh did not work

### Requirement: The next page is loaded before the reader reaches the end

The app SHALL start loading the next page while the reader is still short of the end of the
list, SHALL show that it is loading, and SHALL stop asking once the source has no more
articles.

#### Scenario: Scrolling towards the end

- **WHEN** the reader scrolls to within a few articles of the end and more exist
- **THEN** the next page begins loading before the end is reached
- **AND** the next page is added below what is already there

#### Scenario: The reader arrives before the page does

- **WHEN** the reader reaches the end while the next page is still loading
- **THEN** an indicator is shown at the end of the list

#### Scenario: The end of the source

- **WHEN** the reader reaches the end and the source has no more articles
- **THEN** no further request is made, however many times the end is reached

#### Scenario: The next page fails

- **WHEN** loading the next page fails
- **THEN** the articles already loaded stay on screen
- **AND** the reader is offered a way to try that page again

#### Scenario: No duplicates

- **WHEN** several pages have been loaded
- **THEN** no article appears twice in the list

### Requirement: The detail screen reads what is stored

The app SHALL show an article's detail from what is stored, without contacting the source.

#### Scenario: Opening detail with no network

- **WHEN** an article that is stored is opened with no network
- **THEN** its title, source and body are shown
- **AND** its picture is shown if it was already fetched

#### Scenario: Opening detail for an article that is not stored

- **WHEN** the detail screen is opened for an article that is not stored
- **THEN** an error state is shown with a way to leave the screen
