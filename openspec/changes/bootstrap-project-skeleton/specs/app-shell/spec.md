## Purpose

The app shell is the frame that every feature is drawn inside. It starts the app, lets the
user move between the Reading and Saved tabs, opens and closes an article detail screen, and
draws everything in the light or dark theme the device is set to.

## ADDED Requirements

### Requirement: The app starts on the Reading tab

The app SHALL open on the Reading tab. It SHALL NOT need an account, a sign-in, a
permission, a network connection, or any setup value supplied from outside the project.

#### Scenario: First start with no network

- **WHEN** the app is started for the first time on a device with no network connection
- **THEN** the Reading tab is shown
- **AND** there is no crash, dialog, or prompt that blocks the user

#### Scenario: First start with no setup files

- **WHEN** the app is built and started from a copy of the repository that provides no API
  keys and no local setup files
- **THEN** the app starts and the Reading tab is shown

### Requirement: Moving between the Reading and Saved tabs

The app SHALL always show a navigation bar with exactly two tabs, Reading and Saved. It
SHALL show which of the two is selected.

#### Scenario: Switching to Saved

- **WHEN** the user taps Saved in the navigation bar
- **THEN** the Saved tab is shown
- **AND** the navigation bar shows Saved as selected

#### Scenario: Switching back to Reading

- **WHEN** the user is on the Saved tab and taps Reading
- **THEN** the Reading tab is shown
- **AND** the navigation bar shows Reading as selected

#### Scenario: Tapping the tab that is already open

- **WHEN** the user taps the tab that is already shown
- **THEN** that tab stays shown
- **AND** nothing is added to the back stack

#### Scenario: Each tab keeps its own state

- **WHEN** the user leaves a tab and later comes back to it
- **THEN** the tab is shown in the state it was left in, not reset

### Requirement: The article detail screen

The app SHALL have a detail screen that is opened for one article id. When the user leaves
it, the app SHALL return them to the tab they came from.

#### Scenario: Opening detail from a tab

- **WHEN** an article is tapped on either the Reading or the Saved tab
- **THEN** the detail screen is shown for that article's id

#### Scenario: Returning from detail

- **WHEN** the user leaves the detail screen using either the system back gesture or the
  back button in the app
- **THEN** the tab they came from is shown again, in the state it was left in

#### Scenario: Detail opened with an unknown id

- **WHEN** the detail screen is opened with an id that matches no article
- **THEN** an error state is shown, with a way to leave the screen
- **AND** the app does not crash

### Requirement: The theme follows the system setting

The app SHALL use a light or dark theme according to the device setting, and SHALL apply a
change to that setting without needing a restart. Text SHALL stay readable against its
background in both themes.

#### Scenario: Device set to dark

- **WHEN** the device is set to dark and the app is started
- **THEN** the app is shown in its dark theme

#### Scenario: Theme changed while the app is open

- **WHEN** the device theme is changed while the app is open
- **THEN** the app is shown in the new theme
- **AND** the current screen and its state are kept

### Requirement: One shared set of content states

For any area that shows content, the app SHALL be able to show four states that the user can
tell apart: content is loading, content loaded but there is none, content could not be
loaded because of an error, and content may be old because the device is offline. The error
state and the offline state SHALL each offer the user a way to try again.

#### Scenario: The states look different from each other

- **WHEN** a content area is loading, empty, in error, or offline
- **THEN** what the user sees is clearly different from the other three states

#### Scenario: Retry is offered when something goes wrong

- **WHEN** a content area is in the error state or the offline state
- **THEN** the user has a way to try again

#### Scenario: The states look the same on every screen

- **WHEN** two different screens are in the same state
- **THEN** that state is shown the same way on both
