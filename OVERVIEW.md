# ChargeGuard in plain English

**The problem.** A Pixel Watch 3 needs charging every day. If it runs out in the daytime that is a nuisance. If it runs out overnight, sleep tracking fails.

**The idea.** At 7pm each evening the watch looks at its own battery. If it is 50% or lower and it is not on the charger, the whole screen flashes red and white at full brightness and the watch vibrates in a long repeating pattern, until you put it on the charger, tap the screen, or press the crown. It alerts once only. There is an optional re-check if you tap it away without charging, but it is switched off, because you would not want it going off at a concert or in bed; it can be switched on in the settings file.

**Why it lives on the watch.** The watch can read its own battery reliably, and you are nearly always wearing it. A phone alert could be in another room, and a phone cannot make the watch flash.

**How you change it.** Almost everything you might tweak (the percentage, the time, how fast it flashes, the wording, the vibration, switching it off) is in one small file on GitHub, `config/settings.json`. Edit it, push it, and the watch picks it up next time it opens or does its evening check. No installing. The watch screen shows which settings version and app version it is running. The alert percentage can also be set directly on the watch with a wheel on the status screen, which then beats the file until you choose "Use file". Genuinely new behaviour, such as adding sound, needs a new build installed on the watch.

**What exists.** A working app, tried on a real Pixel Watch 3: it flashes and vibrates on schedule, and settings can be changed from GitHub with no install. "Test in 1 minute" on the watch runs the whole alert within a minute, whatever the battery level, which is handy for demonstrations.

**What is proposed.** A Google Play private testing route so builds reach the watch automatically, and possibly sound.
