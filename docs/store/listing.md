# Google Play store listing (draft, for the Play Console)

App name (max 30): ChargeGuard
Short description (max 80): Flashes and buzzes at 7pm if your watch is too low to last the night.

## Full description (max 4000)
ChargeGuard is for people who wear their watch to bed. If the battery dies overnight, sleep tracking fails. ChargeGuard checks your watch every evening and, if it is low and not on the charger, makes sure you cannot miss it.

At 7pm (you can change the time) the watch checks its own battery. If it is at or below your chosen level, the screen is taken over with full-brightness red and white flashing and a long vibration. It stops when you put the watch on the charger, when you tap it, or when you press the crown.

- One alert only, so no repeat nagging at a concert or in bed
- Set the alert level on the watch with the wheel, or leave it on the default of 50%
- Everything runs on the watch itself. No account, no phone needed, no ads
- Your watch's battery level never leaves the watch

Works on Wear OS 3 and later (tested on Pixel Watch 3).

## Wear OS screenshot
docs/store/wear-screenshot-1-main.png (456 x 456)

## Notes for the Console declarations (to confirm with the owner)
- Full-screen intent: the app's core purpose is an alarm-style alert for low battery.
- Exact alarms: a daily check at a fixed evening time.
- Display over other apps: used to show the alert when the screen is already on.
- Data safety: collects no personal data. Fetches one public settings file from GitHub.
