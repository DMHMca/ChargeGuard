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

(Newer Google Play notes are under "Open decisions" below; the paragraph here is from earlier and still true for adb installs.)

Built by GitHub Actions and installed on the owner's Pixel Watch 3 with adb. Seen working (v1.9): "Test in 1 minute" runs the real alarm path and the watch flashes red/white and vibrates, whatever the screen state. This needed two permissions granted by hand with adb on this watch: `appops set uk.mcaughtry.chargeguard USE_FULL_SCREEN_INTENT allow` and `appops set uk.mcaughtry.chargeguard SYSTEM_ALERT_WINDOW allow` (the watch has no screen to grant overlay manually; the in-app "Over apps" line reads NOT allowed even when it works). A fresh install, including from Google Play, will lack them: that is an open risk. Also seen working: the settings-file fetch (pushed v2, the watch showed v2 after "Refresh settings"; allow a few minutes for GitHub's cache) and the timing (Due, Fired, Posted and Shown all matched in v1.9). The real 19:00 run has also been seen: it fired on its own, exactly on time. Check `docs/CHANGELOG.md` for the latest.

## Open decisions

- **Google Play requirements found (Nov 2026).** Wear OS apps must target API 35 or later (done in v1.18), be uploaded as a signed app bundle (AAB, not the debug APK), add Wear OS as a form factor in the Console, and have at least one Wear OS screenshot and a store listing that mentions Wear OS before review. The Console may also ask for declarations for `USE_FULL_SCREEN_INTENT`, `USE_EXACT_ALARM` and `SYSTEM_ALERT_WINDOW`; and a Play-installed copy will not have the permissions granted by hand with adb here, so the first Play install doubles as the test of whether the alert works without them.
- **Google Play: where it stands (end of 6 Nov 2026).** App entry created (package `uk.mcaughtry.chargeguard`), Wear OS form factor ACTIVE, store listing, icon, banner, 3 Wear OS screenshots, privacy policy (`docs/privacy.md` on GitHub), content rating, target audience (18+), data safety and the other declarations all saved. A signed bundle (v1.19) is live on the INTERNAL testing track for a one-person list "Me only" (the owner's own address); nobody else has been invited and the owner does not want anyone emailed until they have talked to them (adding an address sends no email; the owner shares the opt-in link). The app installs on the owner's Pixel Watch 3 from Play via the direct link `https://play.google.com/store/apps/details?id=uk.mcaughtry.chargeguard` (it does NOT show in Play search; internal-test apps never do). "Send app for review" in the Publishing overview is still greyed out, and the Console's "Monitor and improve" and "Policy and programs" areas are hidden ("Finish setting up your app"), so the full-screen-intent declaration cannot be reached yet. Keep checking it; internal testing works without it. The upload key is `C:\Users\david\ChargeGuard-keys` (outside the repo; password in a text file there) and the GitHub secrets `UPLOAD_KEYSTORE_BASE64` and `UPLOAD_KEYSTORE_PASSWORD` let the build make the signed bundle (artifact `chargeguard-vX-play-bundle`). Each new version still has to be uploaded to Play BY HAND (Console, Test and release, Internal testing, Create new release); automating that needs a service-account key, not done. Sideloaded debug builds and Play builds are signed differently and cannot replace each other: switching means uninstalling first.
- **Play installs and permissions (found 6 Nov).** A Play-installed copy gets the full-screen permission DENIED (Android 14+ only auto-grants it to apps declared as alarm/calling apps) and overlay not allowed, and the Pixel Watch 3 has no settings screen to grant either. So v1.19 from Play only buzzed. v1.20 adds a fallback (wake screen, repeating vibration, notification; tap it for the flashing screen), tested on the watch with the permission denied by hand. The full flash by itself then needs either the Console declaration (core purpose: alarm) or a one-off `adb shell appops set --uid uk.mcaughtry.chargeguard USE_FULL_SCREEN_INTENT allow` plus `appops set uk.mcaughtry.chargeguard SYSTEM_ALERT_WINDOW allow`; the owner finds adb too fiddly for family.
- **Owner's watch right now:** the DEBUG build v1.20 (sideloaded, full-screen allowed by default, so it flashes by itself). Next: upload the v1.20 bundle to Play internal testing, uninstall the debug copy, install from Play and check the fallback on a real Play install. The owner wants their daughter and her partner to use it later, once the owner has asked them.
- **Wireless debugging port** on the watch changes every few hours (`adb connect 192.168.0.97:<port>`); `adb mdns services` sometimes shows it. The owner reads it from Settings, Developer options, Wireless debugging. Use `-s <ip:port>` because adb lists the watch twice.
- **Name.** The owner is considering renaming the app NiteGuard (it guards the night's sleep tracking); decide later, and check the name for clashes first. Renaming only the label shown on the watch is a one-line change (`android:label` in the manifest). Keep the package ID `uk.mcaughtry.chargeguard` and the repository name as they are: changing either would force an uninstall and reinstall, and renaming the repository would break the settings URL the watch fetches from.
- **Sound** alongside flashing and vibration: not added; ask the owner.
- **Threshold**: 45% was the owner's figure; 50% may give a safer margin for a full night of sleep tracking.
