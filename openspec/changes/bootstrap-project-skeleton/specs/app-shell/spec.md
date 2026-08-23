## Purpose

The app shell is the frame every feature is displayed inside: it launches the app, lets
the user move between the top-level Reading and Saved destinations, opens and closes an
article detail view, and renders everything in the light or dark appearance the device
is set to.

## ADDED Requirements

### Requirement: App launches into the Reading destination

The app SHALL launch to the Reading destination without requiring any account, sign-in,
permission grant, network connection, or externally supplied configuration value.

#### Scenario: Cold start with no network

- **WHEN** the app is launched for the first time on a device with no network connection
- **THEN** the Reading destination is displayed
- **AND** no crash, dialog, or blocking prompt is shown

#### Scenario: Cold start with no optional configuration present

- **WHEN** the app is built and launched from a checkout that supplies no API keys or
  local configuration overrides
- **THEN** the app launches and the Reading destination is displayed

### Requirement: Top-level navigation between Reading and Saved

The app SHALL present a persistent top-level navigation control offering exactly two
destinations, Reading and Saved, and SHALL indicate which of the two is currently
selected.

#### Scenario: Switching to Saved

- **WHEN** the user selects Saved from the top-level navigation control
- **THEN** the Saved destination is displayed
- **AND** the control indicates Saved as the selected destination

#### Scenario: Switching back to Reading

- **WHEN** the user is on the Saved destination and selects Reading
- **THEN** the Reading destination is displayed
- **AND** the control indicates Reading as the selected destination

#### Scenario: Re-selecting the destination already shown

- **WHEN** the user selects the destination that is already displayed
- **THEN** that destination remains displayed
- **AND** no additional entry is added to the back stack

#### Scenario: Independent scroll and selection state per destination

- **WHEN** the user leaves a top-level destination and later returns to it
- **THEN** that destination is restored to the state it was left in, rather than reset

### Requirement: Article detail destination

The app SHALL provide a detail destination that is opened for a specific article
identifier and that returns the user to the destination they came from when dismissed.

#### Scenario: Opening detail from a destination

- **WHEN** an article is activated from either the Reading or the Saved destination
- **THEN** the detail destination is displayed for that article's identifier

#### Scenario: Returning from detail

- **WHEN** the user dismisses the detail destination using either the system back
  gesture or the in-app back affordance
- **THEN** the destination the user came from is displayed again, in the state it was
  left in

#### Scenario: Detail opened for an unknown identifier

- **WHEN** the detail destination is opened with an identifier that matches no known
  article
- **THEN** an error state is displayed with a way to leave the destination
- **AND** the app does not crash

### Requirement: Appearance follows the system setting

The app SHALL render in a light or dark appearance according to the device setting, and
SHALL apply a change to that setting without needing to be restarted. Text and its
background SHALL remain legible in both appearances.

#### Scenario: Device set to dark

- **WHEN** the device is set to a dark appearance and the app is launched
- **THEN** the app is displayed in its dark appearance

#### Scenario: Appearance changed while the app is in the foreground

- **WHEN** the device appearance is switched while the app is in the foreground
- **THEN** the app is displayed in the newly selected appearance
- **AND** the user's current destination and its state are preserved

### Requirement: A shared vocabulary of content states

The app SHALL be able to represent, for any content area, each of four states
distinguishably: content is loading, content loaded but is empty, content could not be
loaded because of an error, and content is unavailable or possibly stale because the
device is offline. The error and offline states SHALL each offer the user a way to
retry.

#### Scenario: Each state is visually distinguishable

- **WHEN** a content area is in the loading, empty, error, or offline state
- **THEN** the state presented to the user is distinguishable from the other three

#### Scenario: Retry is offered on failure

- **WHEN** a content area is in the error or the offline state
- **THEN** a retry affordance is available to the user

#### Scenario: States are consistent across destinations

- **WHEN** two different destinations enter the same state
- **THEN** that state is presented consistently in both
