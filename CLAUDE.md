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

A standalone Wear OS app. The watch needs charging daily, and if it dies overnight the sleep tracking fails. Every evening (default 19:00) the watch checks its own battery. If it is at or below the threshold (default 45%) and not on a charger, the screen is taken over: full brightness, flashing red and white, with a long repeating vibration. It stops when the watch is put on the charger. If tapped away instead, it checks again after 30 minutes, up to 23:00.

The app lives on the watch because a watch can read its own battery reliably and the owner nearly always wears it. The phone cannot make the watch flash and may be in another room.

- Repository: `DMHMca/chargeguard` (public by choice, so the watch can fetch its settings file; never commit secrets).
- Package: `uk.mcaughtry.chargeguard`. The name ChargeGuard is shared by an unrelated hardware product; fine for private use, reconsider before any public listing.

## How updates work (two speeds)

**Fast: no install.** `config/settings.json` is the single source of truth for threshold, check hour, on/off, re-check delay, flash speed, alert wording and vibration pattern. The watch fetches it from `Config.SETTINGS_URL` every time the app opens and just before each evening check, and keeps the last good copy. To change behaviour, edit that file, bump its `version` number, and push to `main`. GitHub's file cache can take a few minutes. The watch's status screen shows "Settings vN" and when it last fetched, so the owner can see it arrived. The file only works while the repository is public, so keep it public (or move the file somewhere public and change `Config.SETTINGS_URL`).

**Slower: new build.** Any change to Kotlin code needs a new APK on the watch. A GitHub Actions job (`.github/workflows/build.yml`) builds a debug APK on every push that changes the app, and publishes it as a downloadable artifact. Until the Google Play route is set up, install it with `adb` (see `scripts/deploy.sh`). The planned end state is the Google Play internal testing track, where the watch updates itself from the Play Store.

**Signing.** Debug builds are signed with the fixed throwaway key in `keystore/debug.keystore` (committed on purpose, password `android`, so every build installs over the last). Do not delete or regenerate it, or the watch will refuse updates and need an uninstall. This is separate from the Google Play signing key, which will never be in the repo.

## Rules for every release

1. Bump BOTH `versionCode` and `versionName` in `app/build.gradle.kts`. They are shown on the watch's status screen as "App vN (code)", so the owner can tell which build is running.
2. Bump `version` in `config/settings.json` whenever settings change.
3. Add an entry to `docs/CHANGELOG.md`: what changed, why, and what was observed on the real watch. Do not mark anything as working until it has been seen working.
4. Update `OVERVIEW.md` whenever behaviour changes.
5. Make small commits with plain-English messages and push to `main`. Tell the owner how to see the update.

## Files

- `config/settings.json`: live settings, fetched by the watch.
- `app/src/main/java/uk/mcaughtry/chargeguard/Config.kt`: built-in fallbacks and the settings URL.
- `Prefs.kt`: reads the cached settings and fetches new ones.
- `Scheduler.kt`: the daily alarm (uses `setAlarmClock`, which needs the `USE_EXACT_ALARM` permission, with an inexact fallback) and the re-check.
- `AlarmReceiver.kt`: fetches settings, re-arms tomorrow's alarm, then checks the battery.
- `Alert.kt`: battery reading and the full-screen notification.
- `AlertActivity.kt`: the flashing screen and vibration.
- `MainActivity.kt`: status screen, "Refresh settings", test alert-level override, "Test in 1 minute", "Test alert". A test override (stored on the watch) beats the settings file until "Use settings" is chosen in its picker; the screen shows (TEST) while it is on.
- `docs/CHANGELOG.md`: change and test log.
- `OVERVIEW.md`: plain-English overview.

## Status

Built by GitHub Actions, installed on the owner's Pixel Watch 3 with adb, and seen working: the app opens, and "Test alert" flashes and vibrates. Not yet seen working: the alert waking a dark screen by itself via "Test in 1 minute" (the key test), the full-screen permission being granted for the background case, and the settings file fetch reported on the status screen. Check `docs/CHANGELOG.md` for the latest.

## Open decisions

- **Google Play.** The owner is happy to pay the one-off $25 for a developer account and use the internal testing track, also for future apps. Developer account created on 3 November 2026; waiting for Google's identity and phone verification, after which an app can be created. When it is: switch the build to a signed bundle, upload the first one by hand, then automate uploads from GitHub Actions using a service-account key held as a GitHub secret (never in the repo, never in chat).
- **Sound** alongside flashing and vibration: not added; ask the owner.
- **Threshold**: 45% was the owner's figure; 50% may give a safer margin for a full night of sleep tracking.
