package uk.mcaughtry.chargeguard

/**
 * Built-in fallbacks. The live values come from config/settings.json in the
 * GitHub repo, which the watch fetches (see Prefs.fetch). These defaults are used
 * only for a field the file leaves out, or before the first successful fetch.
 * To change behaviour day to day, edit config/settings.json, not this file.
 */
object Config {
    const val SETTINGS_URL =
        "https://raw.githubusercontent.com/DMHMca/chargeguard/main/config/settings.json"
    const val FETCH_TIMEOUT_MS = 3000

    const val DEFAULT_THRESHOLD = 45        // alert at this % or less
    const val DEFAULT_HOUR = 19             // daily check, 24h clock
    const val RECHECK_MINUTES = 30          // re-check after a tap-dismiss
    const val LAST_RECHECK_HOUR = 23        // no re-checks from this hour on
    const val ALERT_TIMEOUT_MINUTES = 5L
    const val FLASH_INTERVAL_MS = 400L
    const val ALERT_TEXT = "CHARGE\nWATCH"  // battery % is appended on a new line
    val VIBRATION_PATTERN = longArrayOf(0, 700, 250, 700, 250, 1400, 600)
}
