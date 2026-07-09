# Tilezz

Tilezz is a deliberately small Android application that provides one Quick
Settings tile for cycling the phone's actual sound state:

**Do Not Disturb → Vibrate → Normal sound → Do Not Disturb**

The project replaces one useful feature from the abandoned Tiles application.
It is not intended to become a general-purpose Quick Settings tile collection.

## Primary target

- Google Pixel 10a running Android 17
- Current Android SDK and Android Studio/Gradle toolchain
- Kotlin and standard Android APIs
- Minimal dependencies and a simple, readable architecture

Backward compatibility is useful but secondary. The minimum SDK will be chosen
after researching the APIs required for correct modern DND behavior.

## Required behavior

Each tap must inspect the real system state rather than advance an internal
counter:

1. If DND is active, disable DND and switch to vibrate.
2. If DND is inactive and the ringer is in vibrate mode, switch to normal.
3. Otherwise, enable DND.

This keeps the tile correct when state changes through volume controls, Android
Settings, schedules, automation, another application, or a reboot.

The normal state means `AudioManager.RINGER_MODE_NORMAL`; Tilezz must not force
or otherwise modify the user's volume levels.

## Tile presentation

The tile should show the current state using labels such as:

- DND
- Vibrate
- Sound

Use Android vector drawables for appropriate DND, vibration, and speaker icons
where practical. Refresh the display from `onStartListening()` and immediately
after each tap. Tile state, labels, and icons must reflect observed system
state.

## Permissions and first run

Tilezz will likely require
`android.permission.ACCESS_NOTIFICATION_POLICY`. The user must explicitly grant
Notification Policy access.

If access is missing, the application must not crash or fail silently. A tiny
launcher activity may:

- explain the utility;
- show whether DND access is granted;
- open the appropriate Android Settings screen.

No larger settings interface is planned.

## Research first

Before implementation, review current official Android documentation for:

- `TileService` and `Tile`;
- `NotificationManager`;
- `setInterruptionFilter()`;
- `isNotificationPolicyAccessGranted`;
- `ACCESS_NOTIFICATION_POLICY`;
- `AutomaticZenRule`;
- DND changes for applications targeting Android 15 and newer;
- `AudioManager.setRingerMode()`;
- interaction between ringer mode and DND.

The implementation must not assume legacy `setInterruptionFilter()` behavior is
unchanged on modern Android. Research should establish the cleanest supported
approach for Android 17/current Pixels and be summarized concisely in
`RESEARCH.md`.

[TilesOrganization/SimpleTile](https://github.com/TilesOrganization/SimpleTile)
is an architectural reference for the basic Quick Settings service lifecycle,
manifest declaration, `getQsTile()`, and `Tile.updateTile()`. It is not a source
to copy blindly, and old APK binaries should not be reverse-engineered to
recreate obsolete behavior.

## Intended structure

```text
app/src/main/java/.../SoundCycleTileService.kt
app/src/main/java/.../MainActivity.kt
app/src/main/java/.../SoundModeController.kt  # only if it improves clarity
RESEARCH.md
```

`SoundCycleTileService` reads the actual state, performs transitions, and
updates the tile. `MainActivity` handles only the explanation and notification
policy access. A controller helper may isolate DND/ringer logic and API-version
differences if that makes the implementation clearer.

Manifest requirements are expected to include:

- `android.permission.ACCESS_NOTIFICATION_POLICY`;
- a `TileService` guarded by
  `android.permission.BIND_QUICK_SETTINGS_TILE`;
- the `android.service.quicksettings.action.QS_TILE` intent action.

## Development phases

### 1. Research

Inspect SimpleTile and current official Android documentation. Create
`RESEARCH.md` covering reusable architectural concepts, likely causes of the
legacy failure, current DND restrictions, and the proposed implementation.

### 2. Minimal project

Create a fresh Android project with the launcher activity, tile service,
manifest declarations, permission flow, and basic vector icon. Confirm that the
APK builds, installs, and appears in the Quick Settings tile picker.

### 3. State detection

Implement reliable detection of DND, vibrate, and normal modes. Add focused
debug logging and verify that external state changes are observed.

### 4. State switching

Implement and test each transition independently:

- DND → Vibrate
- Vibrate → Normal
- Normal → DND

Pay particular attention to DND → Vibrate and Normal → DND because modern
notification policy behavior is likely to make these transitions the most
complex.

### 5. Tile presentation

Update the icon, label, and active/inactive state according to the actual phone
state. Refresh during `onStartListening()` and after `onClick()`.

### 6. Real-device testing

Build and install a debug APK on the Pixel. Use ADB logs to diagnose failures.
Test:

- fresh installation;
- access missing and access granted;
- every individual transition;
- repeated cycling;
- external sound-mode changes;
- reboot;
- tile removal and re-addition;
- app update over an existing installation.

## Engineering constraints

Prefer:

- Kotlin;
- standard Android SDK;
- AndroidX only where useful;
- Gradle Kotlin DSL for the fresh project;
- minimal dependencies;
- incremental, build-tested changes.

Avoid:

- Jetpack Compose without a concrete need;
- databases, networking, accounts, analytics, ads, or telemetry;
- unrelated background services;
- third-party libraries and architecture frameworks without a clear benefit;
- speculative bulk implementation before API research and build verification.

When behavior requires a physical device, document the exact test and useful
logs rather than claiming it works from build success alone.

## Success criteria

Tilezz is complete when:

- the APK builds with the current Android toolchain;
- it installs on the Pixel 10a;
- its tile can be added normally;
- permission guidance works clearly;
- taps produce DND → Vibrate → Normal → DND;
- transitions use actual system state, not a stored counter;
- presentation reflects current state;
- behavior survives app restarts and phone reboots;
- no unnecessary functionality is included.

Possible future work—configurable order, silent mode, selectable DND modes,
long-press configuration, or additional tiles—is explicitly out of scope for
the first version.
