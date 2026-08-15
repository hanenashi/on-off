# Android platform research

This document records implementation decisions for Tilezz. It is intentionally
limited to behavior that affects the sound-cycle tile.

## Conclusions

### DND is rule-based on modern targets

Applications targeting Android 15 (API 35) or newer cannot directly change the
device's global DND state or policy. Calls to `setInterruptionFilter()` and
`setNotificationPolicy()` are translated into an app-associated
`AutomaticZenRule`. Android combines active rules using the most restrictive
effective policy.

Consequences:

- Tilezz can activate and deactivate a rule that it owns.
- Tilezz cannot disable manual DND, a schedule, or another application's rule.
- `INTERRUPTION_FILTER_ALL` must not be treated as a way to turn off arbitrary
  DND.
- The UI must distinguish Tilezz-owned DND from effective external DND.
- Every requested transition must be followed by a fresh state read.

The preferred design is an explicitly owned `AutomaticZenRule`, stored by rule
ID and recreated if the user deletes it. State changes should use
`setAutomaticZenRuleState()`. On API levels supporting it, the condition should
identify the tile tap as `Condition.SOURCE_USER_ACTION`; this prevents a stale
manual override of Tilezz's rule from swallowing a subsequent explicit tap.

Policy access remains mandatory. Without it, Tilezz should open
`Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` and make no state change.

### Android 17 restricts background ringer changes

Android 17 applies background-audio hardening to all applications running on
the platform. `AudioManager.setRingerMode()` is explicitly covered. When the
application has neither a visible activity nor a qualifying non-short
foreground service, the call can be ignored without an exception.

For applications targeting API 37, a background foreground service additionally
needs while-in-use capability. Android documents user-initiated starts as the
intended route, but does not explicitly guarantee that a Quick Settings tile
tap grants that capability.

Consequences:

- Calling `setRingerMode()` directly in `TileService.onClick()` is not a valid
  Android 17 design without device verification.
- Tilezz needs a narrowly scoped user-initiated foreground execution path.
- A briefly visible activity is the reliable fallback if a service launched
  from the tile does not receive while-in-use capability.
- The implementation must read `ringerMode` after setting it and report a
  failure rather than optimistically updating the tile.
- Pixel testing must inspect Logcat for `AudioHardening` and use
  `adb shell cmd audio set-hardening throw` during development.

An always-running service is out of scope. The foreground component should
exist only for a requested transition and stop immediately afterward.

Pixel 10a live result: direct `TileService.onClick()` work was unreliable when
Tilezz's UI was hidden. Cycling from the main activity worked consistently, and
cycling from the tile became reliable when the tile launched a short-lived
transparent `CycleActivity` in a separate throwaway task. This prevents the
main Tilezz GUI from appearing underneath Quick Settings while preserving a
foreground activity context for ringer changes. A `Theme.NoDisplay` variant was
tested; it avoided surfaces but was not viable because Android requires
NoDisplay activities to finish before `onResume()` returns, and repeated tile
clicks stopped cycling correctly. Tilezz still verifies the actual ringer state
because the DND-to-vibrate transition races Android's asynchronous ringer
restoration when DND is disabled.

### Ringer mode still requires policy access

`AudioManager.setRingerMode()` remains the correct public API for vibrate and
normal modes. Since Android N, changes that cross the DND boundary require
Notification Policy access. Normal mode means `RINGER_MODE_NORMAL`; Tilezz
must not set a volume level.

## State model

Tile presentation and transition decisions need three independent observations:

1. effective global interruption filter;
2. whether Tilezz's own automatic rule is active;
3. current `AudioManager.ringerMode`.

The intended transitions are:

| Observed state | Requested action |
| --- | --- |
| Tilezz rule active | Deactivate it, then request vibrate |
| External DND active | Leave it untouched and direct the user to DND settings |
| DND inactive, ringer vibrate | Request normal |
| DND inactive, any other ringer state | Activate Tilezz's rule |

After each action, re-read all three values. An external rule can keep effective
DND active after Tilezz deactivates its rule.

## Build choices

- `compileSdk` and `targetSdk`: 37, to compile and test against Android 17
  behavior instead of hiding behind an old target.
- `minSdk`: 29, because `setAutomaticZenRuleState()` was introduced in API 29
  and supporting older devices would add a second DND implementation for
  little value.
- Android Gradle Plugin 9.2.x with built-in Kotlin and its required Gradle
  9.4.1 toolchain.
- Framework `Activity` and widgets; no Compose or AndroidX dependency is
  required for the initial utility.

## Required device tests

1. Grant and revoke Notification Policy access.
2. Activate and deactivate Tilezz's rule.
3. Manually enable DND, then confirm Tilezz does not claim to disable it.
4. Exercise DND → vibrate → normal repeatedly.
5. Run with audio hardening set to `throw`; inspect `AudioHardening` Logcat
   entries.
6. Confirm behavior with the screen locked and unlocked.
7. Confirm external schedules and user overrides remain authoritative.
8. Delete Tilezz's automatic rule in Settings and verify recovery.

## Live device notes

Tested on Teneichan, Pixel 10a, Android 17/API 37.

ADB setup:

- wireless ADB paired over Tailscale and connected over both Tailscale and LAN;
- LAN endpoint used for debug commands: `192.168.0.104:35339`.

Validated:

- debug install succeeds;
- `cmd notification allow_dnd net.hanenashi.onoff` grants policy access;
- visible activity cycle:
  `SoundToTilezzDnd`, `TilezzDndToVibrate`, `VibrateToSound`;
- Quick Settings tile cycle through `cmd statusbar expand-settings` +
  `cmd statusbar click-tile net.hanenashi.onoff/.SoundCycleTileService`;
- hardening throw mode via `cmd audio set-hardening throw`;
- DND-to-vibrate needs a short stable-read retry after disabling DND;
- tile clicks should route through `CycleActivity`; doing the work directly in
  `TileService` can fail when the app UI is hidden;
- external DND remains active instead of being cleared by Tilezz.

## Official references

- [Android 15 DND behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15#dnd-changes)
- [`NotificationManager`](https://developer.android.com/reference/android/app/NotificationManager)
- [`AutomaticZenRule`](https://developer.android.com/reference/android/app/AutomaticZenRule)
- [`AudioManager`](https://developer.android.com/reference/android/media/AudioManager)
- [Android 17 background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
