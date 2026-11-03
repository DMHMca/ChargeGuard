# ChargeGuard: project brief for Claude Code

Read this first. It records what the app is, how it is built and updated, the rules the owner has agreed, and what is still to do.

## The owner and how to work with them

- The owner has very little coding experience. Explain what you are doing in plain English, give step-by-step instructions for anything they must do themselves, and say exactly what screen they should see.
- Use British English. Be practical and direct. Prefer short prose over bullet lists when talking to them.
- They are frustrated by fragmented tools and sessions. Keep things simple, tell them what changed after each piece of work, and avoid jargon.
- They have a Google Pixel Watch 3 (45mm), an Android phone and a Windows PC. They also run a photography website, which is unrelated to this project.
- Always give web addresses as CLICKABLE links (markdown links with the full address), never as plain text or code.
- Never ask them to paste secrets into chat.
- They want fast updates with as little install ceremony as possible, like their `get-a-grip` app, where a change is pushed to `main` and goes live.

## What the app is

A standalone Wear OS app. The watch needs charging daily, and if it dies overnight the sleep tracking fails. Every evening (default 19:00) the watch checks its own battery. If it is at or below the threshold (50%, set in the settings file and also the built-in fallback) and not on a charger, the screen is taken over: full brightness, flashing red and white, with a long repeating vibration. It stops when the watch is put on the charger. It alerts once only. An optional re-check after a tap-dismiss exists (`recheckEnabled` in the settings file, OFF by default; if on, every `recheckMinutes` but never at or after `lastRecheckHour`), because the owner does not want repeat alerts at a concert or in bed.

The app lives on the watch because a watch can read its own battery reliably and the owner nearly always wears it. The phone cannot make the watch flash and may be in another room.

- Repository: `DMHMca/chargeguard` (public by choice, so the watch can fetch its settings file; never commit secrets).
- Package: `uk.mcaughtry.chargeguard`. The name ChargeGuard is shared by an unrelated hardware product; fine for private use, reconsider before any public listing.

## How updates work (two speeds)

**Fast: no install.** `config/settings.json` is the single source of truth for threshold, check hour, on/off, re-check delay, last re-check hour, flash speed, alert wording and vibration pattern. The watch fetches it from `Config.SETTINGS_URL` every time the app opens and just before each evening check, and keeps the last good copy. To change behaviour, edit that file, bump its `version` number, and push to `main`. GitHub's file cache can take a few minutes. The watch's status screen shows "Settings vN" and when it last fetched, so the owner can see it arrived. Exception: the alert level can also be set on the watch with the wheel; if so, that value beats `threshold` in the file until "Use file" is chosen, and the status screen says "(set on watch)". The file only works while the repository is public, so keep it public (or move the file somewhere public and change `Config.SETTINGS_URL`).

**Slower: new build.** Any change to Kotlin code needs a new APK on the watch. A GitHub Actions job (`.github/workflows/build.yml`) builds a debug APK on every push that changes the app, and publishes it as a downloadable artifact. Until the Google Play route is set up, install it with `adb` (see `scripts/deploy.sh`). The planned end state is the Google Play internal testing track, where the watch updates itself from the Play Store.

**Signing.** Debug builds are signed with the fixed throwaway key in `keystore/debug.keystore` (committed on purpose, password `android`, so every build installs over the last). Do not delete or regenerate it, or the watch will refuse updates and need an uninstall. This is separate from the Google Play signing key, which will never be in the repo.

## Rules for every release

1. Bump BOTH `versionCode` and `versionName` in `app/build.gradle.kts`. They are shown on the watch's status screen as "App vN (code)", so the owner can tell which build is running.
2. Bump `version` in `config/settings.json` whenever settings change.
3. Add an entry to `docs/CHANGELOG.md`: what changed, why, and what was observed on the real watch. Do not mark anything as working until it has been seen working.
4. Update `OVERVIEW.md` whenever behaviour changes.
5. Make small commits with plain-English messages and push to `main`. Start the commit title with the app version (for example "v1.9: ..."), because GitHub names each build run after its commit title and the owner picks builds from that list. Each run's summary page and its artifact name also show the version. Tell the owner how to see the update.

## Files

- `config/settings.json`: live settings, fetched by the watch.
- `app/src/main/java/uk/mcaughtry/chargeguard/Config.kt`: built-in fallbacks and the settings URL.
- `Prefs.kt`: reads the cached settings and fetches new ones.
- `Scheduler.kt`: the daily alarm (uses `setAlarmClock`, which needs the `USE_EXACT_ALARM` permission, with an inexact fallback) and the re-check.
- `AlarmReceiver.kt`: fetches settings, re-arms tomorrow's alarm, then checks the battery.
- `Alert.kt`: battery reading, the full-screen notification, and a direct launch of the flashing screen (needs "display over other apps").
- `AlertActivity.kt`: the flashing screen and vibration. Stops on tap, on the charger being connected, after 5 minutes, or when the owner presses the crown or side button (the system sends the watch to the watch face; `onStop` with the display still on counts as dismiss).
- `MainActivity.kt`: main screen (battery and status line at the top, then buttons in this order: alert level, refresh settings, the two test buttons, Info), "Refresh settings", "Test in 1 minute" (always fires, whatever the battery, for tests and demos) and "Test alert". The alert level wheel (steps of 5) sets the level on the watch; "Use file" returns control to the settings file. The button and status line say which is in force.
- `InfoActivity.kt` and `Status.kt`: the Info screen with the technical details (permissions, timings, versions), kept off the main screen.
- `docs/CHANGELOG.md`: change and test log.
- `OVERVIEW.md`: plain-English overview.

## Status

Built by GitHub Actions and installed on the owner's Pixel Watch 3 with adb. Seen working (v1.9): "Test in 1 minute" runs the real alarm path and the watch flashes red/white and vibrates, whatever the screen state. This needed two permissions granted by hand with adb on this watch: `appops set uk.mcaughtry.chargeguard USE_FULL_SCREEN_INTENT allow` and `appops set uk.mcaughtry.chargeguard SYSTEM_ALERT_WINDOW allow` (the watch has no screen to grant overlay manually; the in-app "Over apps" line reads NOT allowed even when it works). A fresh install, including from Google Play, will lack them: that is an open risk. Also seen working: the settings-file fetch (pushed v2, the watch showed v2 after "Refresh settings"; allow a few minutes for GitHub's cache) and the timing (Due, Fired, Posted and Shown all matched in v1.9). The real 19:00 run has also been seen: it fired on its own, exactly on time. Check `docs/CHANGELOG.md` for the latest.

## Open decisions

- **Google Play.** The owner is happy to pay the one-off $25 for a developer account and use the internal testing track, also for future apps. Developer account created on 3 November 2026; waiting for Google's identity and phone verification, after which an app can be created. When it is: switch the build to a signed bundle, upload the first one by hand, then automate uploads from GitHub Actions using a service-account key held as a GitHub secret (never in the repo, never in chat).
- **Name.** The owner is considering renaming the app NiteGuard (it guards the night's sleep tracking); decide later, and check the name for clashes first. Renaming only the label shown on the watch is a one-line change (`android:label` in the manifest). Keep the package ID `uk.mcaughtry.chargeguard` and the repository name as they are: changing either would force an uninstall and reinstall, and renaming the repository would break the settings URL the watch fetches from.
- **Sound** alongside flashing and vibration: not added; ask the owner.
- **Threshold**: 45% was the owner's figure; 50% may give a safer margin for a full night of sleep tracking.
