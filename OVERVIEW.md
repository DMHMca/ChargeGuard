# ChargeGuard in plain English

**The problem.** A Pixel Watch 3 needs charging every day. If it runs out in the daytime that is a nuisance. If it runs out overnight, sleep tracking fails.

**The idea.** At 7pm each evening the watch looks at its own battery. If it is 45% or lower and it is not on the charger, the whole screen flashes red and white at full brightness and the watch vibrates in a long repeating pattern, until you put it on the charger. If you tap it away without charging, it asks again 30 minutes later, but never at or after 9pm (adjustable in the settings file), so it cannot go off while you are asleep.

**Why it lives on the watch.** The watch can read its own battery reliably, and you are nearly always wearing it. A phone alert could be in another room, and a phone cannot make the watch flash.

**How you change it.** Almost everything you might tweak (the percentage, the time, how fast it flashes, the wording, the vibration, switching it off) is in one small file on GitHub, `config/settings.json`. Edit it, push it, and the watch picks it up next time it opens or does its evening check. No installing. The watch screen shows which settings version and app version it is running. Genuinely new behaviour, such as adding sound, needs a new build installed on the watch.

**What exists.** The first version is written but has not yet been built or tried on a real watch.

**What is proposed.** A Google Play private testing route so builds reach the watch automatically, and possibly sound.
