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

## 1.14 - alert level wheel back, as a permanent setting
- Owner wants to set the level on the watch without going to GitHub. Brought the wheel back (steps of 5) but as a plain, visible setting: "Set" stores the level on the watch, "Use file" hands control back to config/settings.json.
- The button says "Alert at X% (set here)" or "(from file)", and the Armed line adds "(set on watch)", so it is never a hidden override. A watch-set level beats `threshold` in the file.
- Stored under a new key, so the old 45 test override from earlier builds does not come back. "Test in 1 minute" still always fires.

## 1.13 - test override removed; "Test in 1 minute" always fires
- Owner: the on-watch level override is no longer needed (the alert is proven) and would silently beat the settings file. Removed the picker and override entirely; the alert level now comes only from config/settings.json.
- "Test in 1 minute" now always fires whatever the battery, so a test or demonstration needs no override.
- Built-in fallback threshold aligned to 50 (the file's value). Supersedes 1.12 (same changes plus this).

## Settings v5 - threshold 50%
- Owner: 50% at 7pm should last the night. Changed `threshold` from 45 to 50 in the settings file only, so it reaches any installed build with no install. (The built-in fallback in Config.kt is still 45 and will be aligned in the next build.)
- Note: a test override left set on the watch beats the file until "Use settings" is chosen in the alert-level picker.

## 1.12 - one alert only (re-checks off by default)
- Owner: the 19:00 test fired exactly on schedule. A re-check alert is not wanted (could go off at a concert or in bed); one alert at 19:00 is enough, and the threshold can be adjusted if it proves too low.
- New setting `recheckEnabled` (default false) in config/settings.json. When true, the earlier behaviour returns: after a tap-dismiss it re-checks every `recheckMinutes`, never at or after `lastRecheckHour`. Needs this build once (settings v4). Supersedes 1.11 (the same build plus this).
- Verified by owner: the real 19:00 alarm fired on its own, exactly on time.

## 1.11 - no alerts late in the evening
- Owner: the 19:00 alert was tapped away, and a re-check alert came at 19:30 (below threshold). They do not want alerts while asleep; re-checks used to run until 23:00.
- New setting `lastRecheckHour` in config/settings.json (default 21): re-checks stop before this hour, so the last possible re-check alert is before 21:00. The 19:00 alert itself is unaffected. Needs this build installed once; after that the hour can be changed from the settings file (settings v3).

## 1.10 - "Refresh settings" now says what happened
- Owner pressed Refresh after settings v2 was pushed: still showed v1 and there was no message, so success and failure looked the same.
- Now it buzzes and shows "Settings updated: vN" or "Couldn't reach GitHub. Check the watch's Wi-Fi."

## Settings v2 - fast-update test
- Version bumped from 1 to 2 with no other change, to prove the no-install loop: push to main, press Refresh settings, status shows "Settings v2".
- RESULT: confirmed working. After a few minutes (an early refresh still showed v1, most likely GitHub's file cache), "Refresh settings" on the watch showed v2 with no install. Allow up to about five minutes after a push.
- Before this: the owner's watch showed "Settings v1", which can only come from a fetched file (the built-in fallback reads v0), so the first fetch is confirmed working.

## Test results so far (owner, real Pixel Watch 3)
- 1.2: app opens; "Test alert" flashes and vibrates.
- 1.4: "Test in 1 minute" vibrated only (no flash) until USE_FULL_SCREEN_INTENT was granted by hand with adb; after that it flashed, but late and only when the screen was dark.
- 1.8: vibrated, no flash (screen possibly on).
- 1.9: WORKS. Flashes red/white and vibrates from "Test in 1 minute" with the screen in any state, after `appops set uk.mcaughtry.chargeguard SYSTEM_ALERT_WINDOW allow` (status line "Over apps" still says NOT allowed on this watch; the appop read-back says allow, and the alert works regardless).
- Still to verify: the real 19:00 run at a real battery level; the settings-file fetch; how late (if at all) the alert is, from the Due/Fired/Posted/Shown lines.
- Known risk: both permissions (full-screen, and overlay via appops) were granted with adb. This watch has no screen to grant overlay by hand. A Play Store install will not have them, so before relying on the Play route we must find how to get the alert to work without adb, or document the adb step.

## 1.9 - alert must flash even if the screen is on
- Owner test on 1.8: vibrated but no flashing; the watch may not have been dark. A full-screen notification only takes over an idle screen; with the screen on Android shows a pop-up instead, so the alert depended on screen state.
- Now the app also launches the flashing screen directly when the alarm fires. This needs "display over other apps" (SYSTEM_ALERT_WINDOW); declared, requested once on first open, and shown as "Over apps" on the status screen. Grant by hand if the watch has no such screen: `adb shell appops set uk.mcaughtry.chargeguard SYSTEM_ALERT_WINDOW allow`.
- Untested on the watch.

## 1.8 - four timestamps on the status screen
- Correction: the owner's 1.4 test had no confirmation buzz, so the vibration was the alert's notification, and the flashing screen came 1-2 minutes after it. The delay is in the takeover launching, not (only) the alarm.
- Status screen now shows Due, Fired (alarm delivered), Posted (notification posted) and Shown (flashing screen created), to show which step is late.

## 1.7 - alert no longer waits for the network
- Owner test on 1.6: full flash and vibrate worked, but about two minutes late. Suspected cause: the alarm receiver fetched settings first, and a sleeping watch can stall on the address lookup (not covered by the connect/read timeouts).
- Now: re-arm tomorrow, alert immediately from saved settings, then fetch fresh settings afterwards.
- Status screen shows "Test due" and "Alarm fired" times so a late alarm can be told apart from a late alert. Not yet confirmed which it was.
- "wrist down" wording changed to "screen off".

## 1.6 - clear test feedback and permission prompt
- First wrist-down test on 1.4: vibrated only, no flash, no message. Cause found with adb: `appops get ... USE_FULL_SCREEN_INTENT` showed `default` with a recent rejectTime, so Android refused the full-screen takeover. Granting it with `appops set ... allow` was then done by hand (re-test result to be recorded).
- "Test in 1 minute" now buzzes, shows a message and counts down on the button.
- Status screen has a "Takeover: allowed / NOT allowed" line; the app opens the permission screen once on first run if not allowed (the screen may not exist on every watch; untested).

## 1.5 - picker in steps of 5
- Owner asked for 5% steps instead of every digit, so the wheel is quicker to spin. Wheel now shows 5, 10 ... 100.
- 1.4 observed on the watch by the owner: installed and "works ok"; the wrist-down test (Test in 1 minute) was still waiting at the time of writing.

## 1.4 - proper level picker
- The alert-level button now opens a swipe wheel (5 to 100) with Set / Use settings / Cancel, replacing tap-to-cycle from 1.3 (clumsy; owner asked for a dial).

## 1.3 - test controls on the watch
- Status screen: "Alert at X%" button sets a temporary test override (tap-to-cycle in 1.3, a wheel from 1.4). Status shows (TEST) while it is on.
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
