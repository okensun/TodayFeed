## Purpose

Saving is the reader saying "keep this one". Everything else in the app decides for itself how
long content is worth holding; this is the one thing the reader decides. So what they save stays
until they say otherwise, and stays readable when there is no network at all.

## ADDED Requirements

### Requirement: A reader can save an article and unsave it

The app SHALL let a reader save an article, and unsave it again, from the feed and from the
article screen. The control SHALL show whether the article is saved, so the reader can tell one
from the other without opening it. Saving and unsaving SHALL take effect wherever that article is
shown, without the reader leaving the screen they are on.

#### Scenario: Saving from the feed

- **WHEN** the reader saves an article from a card in the feed
- **THEN** that card shows the article as saved
- **AND** the article appears in the Saved tab

#### Scenario: Saving from the article screen

- **WHEN** the reader saves an article while reading it
- **THEN** the control on that screen shows it as saved
- **AND** returning to the feed shows the same article as saved

#### Scenario: Unsaving

- **WHEN** the reader unsaves an article
- **THEN** the control shows it as not saved
- **AND** the article is no longer in the Saved tab

#### Scenario: Unsaving from the Saved tab

- **WHEN** the reader unsaves an article while looking at the Saved tab
- **THEN** it leaves the list
- **AND** the rest of the list keeps its order and its position

### Requirement: The Saved tab shows what was saved, most recently saved first

The app SHALL show saved articles in the Saved tab, ordered by when they were saved, most recent
first. With nothing saved, the tab SHALL say how to save something rather than report a failure.

#### Scenario: Nothing saved yet

- **WHEN** the Saved tab is opened and nothing has been saved
- **THEN** it says that saving an article puts it here
- **AND** it does not show an error or a retry

#### Scenario: Order

- **WHEN** two articles are saved, one after the other
- **THEN** the Saved tab shows the more recently saved one first, whatever their published dates

### Requirement: Saved articles are readable with no network

The app SHALL show a saved article, and its text, with no network. Opening a saved article SHALL
NOT depend on a request succeeding.

#### Scenario: Saved and then offline

- **WHEN** an article has been saved, and the device then has no network
- **THEN** the Saved tab still lists it
- **AND** opening it shows the article rather than an offline message

#### Scenario: Offline from the start of the session

- **WHEN** the app is opened with no network and articles were saved in an earlier session
- **THEN** the Saved tab lists them

### Requirement: Saving outlives the freshness policy

A saved article SHALL remain available until the reader unsaves it. No refresh, no expiry and no
tidying of cached content SHALL remove it. The freshness policy decides when to ask the source
for more; it SHALL have no say over what the reader chose to keep.

#### Scenario: A refresh does not disturb what is saved

- **WHEN** the feed is refreshed after articles were saved
- **THEN** every saved article is still saved
- **AND** the Saved tab is unchanged apart from any article saved since

#### Scenario: An article that leaves the feed

- **WHEN** a saved article is no longer among the articles the source returns
- **THEN** it is still in the Saved tab
- **AND** it can still be opened and read
