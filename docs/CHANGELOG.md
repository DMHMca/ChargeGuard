# ChargeGuard change and test log

Newest first. One entry per change: what changed, why, and what was observed
on the real watch. "Untested" is a valid status; do not mark anything working
until it has been seen working.

## Open questions / ideas
- Does the alert fire from a dimmed/locked screen at the check time? (the key test)
- Does Wear OS grant the full-screen notification permission by default?
- Does the background settings fetch work at 19:00, or does the watch have no network then?
- Is 45% enough for a full night of sleep tracking, or should it be 50%?
- Sound as well as flashing and vibration (not added; wanted?).

## 1.3 - test controls on the watch
- Status screen: "Alert at X%" button cycles a temporary test override (50 to 100, then back to the settings file). Status shows (TEST) while it is on.
- "Test in 1 minute" button runs the real alarm path (alarm, battery check, full-screen takeover) so it can be watched with the wrist down.
- Verified on the watch before this: app opens (1.2), Test alert flashes and vibrates.
- Not yet verified: the alert waking a dark screen by itself; the settings fetch (user to report the Settings line).

## 1.2 - fixed signing key
- Reinstalling 1.1 failed with INSTALL_FAILED_UPDATE_INCOMPATIBLE because every GitHub build made a new random debug key.
- Added keystore/debug.keystore (throwaway debug key, deliberately committed) and pointed the debug build at it, so all builds from here are signed identically and install over each other.
- One-off: the old install must be uninstalled once (`adb uninstall uk.mcaughtry.chargeguard`) because it carries a different key.

## 1.1 - fix crash on opening
- First install on the real watch crashed at once: Android refused the exact alarm (SecurityException: needs SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM). Earlier belief that setAlarmClock needs no permission was wrong.
- Fix: declare USE_EXACT_ALARM; catch SecurityException and fall back to an inexact alarm. Untested on the watch until reinstalled.
- Note: USE_EXACT_ALARM is meant for alarm/calendar apps; fine for private use, revisit before any public Play listing.
- Observed: build compiled first time and installed by adb (green).

## Repo made public
- Needed so the watch can fetch config/settings.json from the raw GitHub URL. Confirmed the URL now returns the file.

## 1.0 (settings v1) - first version, untested
- Watch-only app. Daily check at 19:00 via setAlarmClock.
- Alert at or below 45% when not charging: red/white flash, repeating vibration.
- Stops on charger connect, tap (re-checks in 30 min until 23:00), or 5 min timeout.
- Settings fetched from `config/settings.json` on GitHub (cached on the watch); status screen shows settings and app versions.
- Package `uk.mcaughtry.chargeguard`. GitHub Actions builds a debug APK on push.
- Written without an Android SDK; not compiled, not run on a watch.
