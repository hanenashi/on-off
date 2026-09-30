# 音OFF handoff

## October 2026 launcher update

- The home-screen control is now a pinned shortcut (`cycle`) with a stable target, `LauncherSound`. Its artwork changes with the observed sound mode.
- The regular app-drawer icon stays fixed, still cycles modes, and retains the long-press Settings action. Settings includes an Add home-screen shortcut button in English, Czech, and Japanese.
- `LauncherShortcutController.kt` replaces `LauncherIconController.kt`. Never disable launcher components on mode changes: Microsoft Launcher was observed launching stale disabled aliases and reporting “App isn’t installed”, including after trying atomic batch changes.
- Old `LauncherVibrate`/`LauncherDnd` aliases remain enabled with no launcher intent filters. `AppUpdateReceiver` clears persisted component overrides on upgrade; activity paths also perform this migration if necessary.
- `TapFeedback.kt` provides an optional double buzz (90 ms on, 80 ms off, 150 ms on), enabled by default. It uses touch-vibration attributes and respects Android touch-feedback settings.
- Users upgrading from alias-based icons should replace the old home-screen icon with the pinned shortcut once. The earlier sections below describe historical releases.
- Live checks on Teneichan / Microsoft Launcher: pinned shortcut added and moved into the old icon position; repeated launches retained the same target without the former `-92` missing-activity errors; double-buzz waveform completed. Microsoft Launcher does not show the app Settings action on the pinned shortcut itself; long-press the regular 音OFF entry in the app drawer for Settings.

## Current state

音OFF is a small Android utility that cycles the device's real sound state from its launcher icon. The default cycle is Sound ↔ Vibrate. Users can optionally include an app-associated Do Not Disturb step.

- Repository: `https://github.com/hanenashi/on-off`
- Local checkout: `C:\GIT\on-off`
- Default branch: `main`
- Latest pushed commit at handoff: `e237c32` (`Prepare on-off identity and privacy policy`)
- Live site: `https://hanenashi.github.io/on-off/`
- Privacy policy: `https://hanenashi.github.io/on-off/privacy.html`

## Important identity decision

The Android package and Kotlin namespace were deliberately renamed from the legacy `net.hanenashi.tilezz` identity to:

```text
net.hanenashi.onoff
```

This is the intended permanent package name for a future Google Play listing. Do not change it casually after the app is registered in Play Console.

The Kotlin files now live under:

```text
app/src/main/java/net/hanenashi/onoff/
```

The manifest task affinity and launcher shortcut targets were updated to the new package as well.

## Android configuration

From `app/build.gradle.kts`:

- `compileSdk = 37`
- `targetSdk = 37`
- `minSdk = 29`
- `versionCode = 2`
- `versionName = "1.0.1"`

The current downloadable GitHub release asset is still the debug APK named:

```text
on-off-v1.0.1-debug.apk
```

Google Play preparation still needs a private upload key, secure release-signing configuration, and a signed Android App Bundle (`.aab`). Do not commit a keystore or signing passwords.

## Main Android components

- `CycleActivity.kt`: tiny transparent foreground activity used by the launcher action.
- `MainActivity.kt`: settings screen.
- `SoundCycleController.kt`: reads the actual Android sound/DND state and performs transitions.
- `LauncherIconController.kt`: enables the Sound, Vibrate, or DND launcher alias matching the observed state.
- `ModeToast.kt`: shows the resulting sound mode.
- `LocaleController.kt`: handles system-default or explicit English, Japanese, and Czech language selection.

The launcher aliases are `LauncherSound`, `LauncherVibrate`, and `LauncherDnd`. The launcher currently resolves to `net.hanenashi.onoff/.LauncherSound` on a fresh install.

## Website

`index.html` is the GitHub Pages landing page. It is intentionally compact enough to fit within one desktop viewport while remaining scrollable on mobile.

The page includes:

- English, Czech, and Japanese content.
- APK download and GitHub source links.
- A smaller three-state icon stage.
- Side-by-side Why and How-to-use panels on desktop.
- A short explanation of the name:
  - `音` means sound.
  - `音` reads as “on.”
  - `ON–OFF` describes what the app switches.
- A footer link to `privacy.html`.

`privacy.html` states the current behavior accurately: no data collection, transmission, analytics, advertising, accounts, tracking, or third-party data-collection SDKs. Sound and DND state processing happens locally. If these practices change, update the policy before releasing the changed app.

## Validation already completed

The following command completed successfully after the package rename:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\hanenashi\AppData\Local\Android\Sdk'
.\gradlew.bat clean test lint assembleDebug
```

Notes:

- There are currently no unit-test sources, so the unit-test task reports `NO-SOURCE`.
- Lint passes.
- The build has existing Android deprecation warnings around `overridePendingTransition` and the custom Toast view API; they do not currently fail the build.
- `git diff --check` passed before the last push.

## Pixel test device

A Pixel 10a was connected over USB ADB:

```text
serial: 5A031JEA300189
model: Pixel_10a
device: stallion
```

The user removed the old app before the package migration test. The renamed debug APK installed successfully as `net.hanenashi.onoff`, and `MainActivity` opened from a cold launch.

Because the renamed app is a fresh package, its Do Not Disturb access is not granted until the user explicitly enables it again in Android Settings.

## Website validation

The live GitHub Pages deployment for commit `e237c32` completed successfully. The live endpoints returned HTTP 200 and contained the expected name explanation, privacy link, and privacy-policy content.

The landing page was visually checked in Chrome at desktop and mobile sizes. English, Czech, and Japanese fit within one desktop viewport. Mobile remains scrollable without horizontal overflow. The privacy page was also checked on desktop and mobile.

Console warnings seen during browser QA came from an installed Chrome extension, not from the site.

## Recommended Google Play next steps

1. Keep `net.hanenashi.onoff` as the permanent application ID.
2. Create a Play Console developer account and complete identity/device verification.
3. Generate and securely back up a private upload keystore.
4. Add release signing without placing secrets in Git.
5. Build and test a signed release `.aab` on the Pixel through a Play testing track.
6. Prepare the Play Store listing: title, descriptions, 512×512 icon, 1024×500 feature graphic, and phone screenshots.
7. Complete App Content, Data Safety, ads, target-audience, content-rating, and privacy-policy declarations.
8. If using a new personal Play developer account, plan for the required closed test before requesting production access.

## Working-tree expectation

At the time this handoff file was created, all previously completed application and website changes were already committed and pushed. This `handoff.md` file itself may be the only new uncommitted file. Check with:

```powershell
git status -sb
```
