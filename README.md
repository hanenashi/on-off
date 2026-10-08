# 音OFF

**Website:** [hanenashi.github.io/on-off](https://hanenashi.github.io/on-off/)

音OFF is a deliberately small Android utility for cycling the phone's real sound state from a home-screen widget or app icon.

Default cycle:

**Sound ↔ Vibrate**

Optional cycle when DND is enabled in settings:

**Sound → DND → Vibrate → Sound**

The project started as a replacement for one useful feature from the abandoned Tiles app, but it is now intentionally launcher-only. Quick Settings tile cycling was removed because modern Android background restrictions made it less reliable than the home-screen icon path.

## Current behavior

- Tapping the 音OFF widget or launcher icon cycles immediately and exits.
- Launcher taps give two vibration pulses before cycling, enabled by default and switchable in Settings under Tap feedback. Android touch-feedback settings still apply.
- Long-pressing the launcher icon exposes a Settings shortcut.
- Settings can include or exclude DND and Vibrate from the cycle.
- Settings default to the Android system language and can explicitly override
  音OFF to English, Japanese, or Czech.
- Defaults are DND excluded and Vibrate included.
- Settings → Add mode widget requests a one-cell home-screen widget. Its black-circle Sound, Vibrate, and DND images update directly, avoiding Pixel Launcher's pinned-shortcut image cache. Remove the old home-screen shortcut after placing the widget.
- Settings → Widget alignment has horizontal and vertical controls for the artwork inside the existing widget cell. Changes update placed widgets immediately and persist across app restarts; Reset restores the Pixel-aligned default of 8 dp up. The full widget cell remains the tap target.
- Existing pinned shortcuts remain supported as a legacy option. Their tap target stays fixed and cycles even if Pixel Launcher freezes the displayed shortcut image after a language change.
- The regular app-drawer icon stays fixed and still cycles modes; long-press it for Settings.
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
app/src/main/java/net/hanenashi/onoff/ModeWidgetProvider.kt
```

`CycleActivity` is a tiny transparent foreground activity used for launcher and widget taps. `MainActivity` is the settings screen. `SoundCycleController` owns the DND/ringer-mode transition logic. `ModeWidgetProvider` pushes `RemoteViews` updates directly to placed widgets after each cycle and when Settings opens. `LauncherShortcutController` still updates the legacy pinned `cycle` shortcut without changing its identity or disabling launcher components.

The `1.0.2-rc2` test build added the widget after the shortcut image froze again on a Pixel 10a following an app-language change. Its symbols are native vector drawables on black circles; the old phone-frame artwork remains in `assets/` as source history. Settings still shows Android's shortcut-update acceptance status for diagnostics. The widget passed Android 15 Pixel Launcher emulator tests through repeated mode taps and app-language changes, and the affected physical phone confirmed immediate updates. The `1.0.2-rc3` build shifted the artwork upward within its unchanged one-cell touch target to align with neighboring Pixel Launcher icons. `1.0.2-rc4` makes that position adjustable in Settings.

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
