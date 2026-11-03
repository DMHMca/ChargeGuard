# ChargeGuard

A standalone Wear OS app for a Pixel Watch. Every evening (default 19:00) the watch checks its own battery. At or below the threshold (default 45%) and not charging, the screen flashes red and white at full brightness with a long repeating vibration, until the watch goes on the charger.

See `OVERVIEW.md` for the plain-English version and `CLAUDE.md` for the project brief and release rules.

## Changing behaviour (no install)

Edit `config/settings.json`, raise its `version` number and push to `main`. The watch fetches it when the app opens and before each evening check.

## Getting a new build onto the watch

Code changes need a new APK. Each push to `main` that changes the app is built by GitHub Actions; download `app-debug.apk` from the run's Artifacts. Then, with the watch's Wireless debugging on:

```bash
scripts/deploy.sh <watch-ip:port> path/to/app-debug.apk
```

First time only, pair with `adb pair <ip:pairing-port>`. Wireless debugging is under Settings > Developer options on the watch (tap Build number seven times in Settings > System > About > Versions to reveal it).

After installing, open ChargeGuard once so it arms the alarm and asks for notification permission. "Test alert" shows the flashing screen.

## Status

Written but never compiled or run on a watch. See `docs/CHANGELOG.md`.
