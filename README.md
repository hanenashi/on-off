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
- Launcher taps give two vibration pulses before cycling, enabled by default and switchable in Settings under Tap feedback. Android touch-feedback settings still apply.
- Long-pressing the launcher icon exposes a Settings shortcut.
- Settings can include or exclude DND and Vibrate from the cycle.
- Settings default to the Android system language and can explicitly override
  音OFF to English, Japanese, or Czech.
- Defaults are DND excluded and Vibrate included.
- Settings → Add home-screen shortcut creates a pinned shortcut with Sound, Vibrate, and DND artwork. Replace the old home-screen app icon with this shortcut once.
- The pinned shortcut keeps the same ID and tap target while its image updates. A delayed image refresh cannot disable the button. The regular app-drawer icon stays fixed and still cycles modes.
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
app/src/main/java/net/hanenashi/onoff/CycleActivity.kt
app/src/main/java/net/hanenashi/onoff/MainActivity.kt
app/src/main/java/net/hanenashi/onoff/SoundCycleController.kt
app/src/main/java/net/hanenashi/onoff/LauncherShortcutController.kt
```

`CycleActivity` is a tiny transparent foreground activity used for the launcher action. `MainActivity` is the settings screen. `SoundCycleController` owns the DND/ringer-mode transition logic. `LauncherShortcutController` updates the pinned `cycle` shortcut without changing its identity or disabling launcher components.

`LauncherSound` is the permanent app-drawer entry. Legacy `LauncherVibrate` and `LauncherDnd` aliases remain enabled but have no launcher intent filters, allowing cached old explicit intents to work without creating extra app-drawer icons. `AppUpdateReceiver` and the activity entry paths reset old persisted disabled-component overrides during migration. Do not restore mode-dependent alias toggling: Microsoft Launcher can retain the disabled target and show “App isn’t installed” during refresh.

The Android package is `net.hanenashi.onoff`.

## Validation

Verified on Teneichan, a Pixel 10a running Android 17/API 37:

- debug APK builds and installs successfully;
- launcher icon cycles Sound ↔ Vibrate with DND excluded;
- optional DND cycle enters and exits 音OFF's app-associated DND rule;
- stale internal DND preference state does not leave 音OFF-owned DND stuck on;
- external/manual DND remains active when 音OFF does not own the active DND state;
- app display label is `音OFF`;
- pinned shortcut uses mode-specific artwork with a stable launch target;
- settings localization works for System default, Japanese, and Czech.
