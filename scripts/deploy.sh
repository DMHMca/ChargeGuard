#!/usr/bin/env bash
# Build and install ChargeGuard on the watch in one step.
# Usage: scripts/deploy.sh <watch-ip:port> [path-to-apk]
# First time only, pair with: adb pair <ip:pairing-port>
set -euo pipefail
cd "$(dirname "$0")/.."

WATCH="${1:?usage: scripts/deploy.sh <watch-ip:port>}"
adb connect "$WATCH"
echo "Download app-debug.apk from the latest GitHub Actions run (Artifacts), or build locally with ./gradlew :app:assembleDebug"
adb -s "$WATCH" install -r "${2:-app/build/outputs/apk/debug/app-debug.apk}"
adb -s "$WATCH" shell am start -n uk.mcaughtry.chargeguard/.MainActivity
echo "Installed. Add a line to docs/CHANGELOG.md for what you changed and tested."
