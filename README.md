# 音OFF

**Website:** [hanenashi.github.io/on-off](https://hanenashi.github.io/on-off/)

音OFF is a deliberately small Android utility for cycling the phone's real sound state from the home-screen app icon.

Default cycle:

**Sound ↔ Vibrate**

Optional cycle when DND is enabled in settings:

**Sound → DND → Vibrate → Sound**

The project started as a replacement for one useful feature from the abandoned Tiles app, but it is now intentionally launcher-only. Quick Settings tile cycling was removed because modern Android background restrictions made it less reliable than the home-screen icon path.

## Current behavior

- Tapping the 音OFF launcher icon cycles immediately and exits.
- Long-pressing the launcher icon exposes a Settings shortcut.
- Settings can include or exclude DND and Vibrate from the cycle.
- Settings default to the Android system language and can explicitly override
  音OFF to English, Japanese, or Czech.
- Defaults are DND excluded and Vibrate included.
- The launcher icon switches between Sound, Vibrate, and DND aliases after 音OFF observes the current mode. Android launchers may cache icon state, so visual refresh timing is launcher-dependent.
- Each successful tap shows a short Toast naming the resulting mode.

## Primary target

- Google Pixel 10a running Android 17/API 37
- Current Android SDK and Android Studio/Gradle toolchain
- Kotlin and standard Android APIs
- Minimal dependencies and a simple, readable architecture

Backward compatibility is useful but secondary.

## Required behavior

Each tap must inspect the real system state rather than advance an internal counter:

1. If DND is active, 音OFF tries to deactivate its own app-associated DND rule.
2. If another DND source remains active, report external DND instead of claiming the transition succeeded.
3. If DND is inactive and the ringer is in vibrate mode, switch to normal sound.
4. If DND is enabled in app settings, activate 音OFF's DND rule.
5. Otherwise, switch to vibrate when Vibrate is enabled.

This keeps the app correct when state changes through volume controls, Android Settings, schedules, automation, another application, or a reboot. Android 15+ does not let 音OFF disable a DND rule owned by the user, the system, or another application.

The normal state means `AudioManager.RINGER_MODE_NORMAL`; 音OFF must not force or otherwise modify the user's volume levels.

## Android DND behavior

Android 15+ changed DND control for apps targeting API 35 or newer. Calls to `NotificationManager.setInterruptionFilter()` create or toggle an app-associated `AutomaticZenRule` instead of directly owning global DND. 音OFF therefore treats the Android-observed state as authoritative and deactivates its own rule before deciding whether remaining DND is external.

## Permissions and settings

音OFF uses `android.permission.ACCESS_NOTIFICATION_POLICY` when DND is included in the cycle. The user must explicitly grant Notification Policy access.

If access is missing, the app opens its settings screen and provides a button to Android's Do Not Disturb access settings.

## Implementation notes

Important files:

```text
app/src/main/java/net/hanenashi/tilezz/CycleActivity.kt
app/src/main/java/net/hanenashi/tilezz/MainActivity.kt
app/src/main/java/net/hanenashi/tilezz/SoundCycleController.kt
app/src/main/java/net/hanenashi/tilezz/LauncherIconController.kt
```

`CycleActivity` is a tiny transparent foreground activity used for the launcher icon action. `MainActivity` is the settings screen. `SoundCycleController` owns the DND/ringer-mode transition logic. `LauncherIconController` switches the enabled launcher alias so the home-screen icon reflects the observed mode.

The Android package remains `net.hanenashi.tilezz` to avoid reinstall/permission churn from package migration.

## Validation

Verified on Teneichan, a Pixel 10a running Android 17/API 37:

- debug APK builds and installs successfully;
- launcher icon cycles Sound ↔ Vibrate with DND excluded;
- optional DND cycle enters and exits 音OFF's app-associated DND rule;
- stale internal DND preference state does not leave 音OFF-owned DND stuck on;
- external/manual DND remains active when 音OFF does not own the active DND state;
- app display label is `音OFF`;
- launcher aliases use mode-specific icons;
- settings localization works for System default, Japanese, and Czech.
