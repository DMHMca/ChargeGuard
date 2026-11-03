package uk.mcaughtry.chargeguard

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The watch's settings. The single source of truth is config/settings.json on
 * GitHub; the last good copy is cached on the watch so a missing connection at
 * check time never stops the alert. Any field missing from the file falls back
 * to Config.
 */
object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private fun json(c: Context): JSONObject = try {
        JSONObject(p(c).getString("remote", "{}") ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    /** Alert level from the settings file. */
    fun fileThreshold(c: Context) = json(c).optInt("threshold", Config.DEFAULT_THRESHOLD)

    /** Alert level chosen on the watch with the wheel, or -1 if the settings file is in charge. */
    fun watchThreshold(c: Context) = p(c).getInt("watchThreshold", -1)
    fun setWatchThreshold(c: Context, v: Int) = p(c).edit().putInt("watchThreshold", v).apply()

    /** The level actually used: the watch's own choice if there is one, else the settings file. */
    fun threshold(c: Context): Int {
        val w = watchThreshold(c)
        return if (w >= 0) w else fileThreshold(c)
    }

    fun hour(c: Context) = json(c).optInt("hour", Config.DEFAULT_HOUR)
    fun enabled(c: Context) = json(c).optBoolean("enabled", true)
    fun recheckEnabled(c: Context) = json(c).optBoolean("recheckEnabled", Config.RECHECK_ENABLED)
    fun lastRecheckHour(c: Context) = json(c).optInt("lastRecheckHour", Config.LAST_RECHECK_HOUR)
    fun recheckMinutes(c: Context) = json(c).optInt("recheckMinutes", Config.RECHECK_MINUTES)
    fun flashIntervalMs(c: Context) = json(c).optLong("flashIntervalMs", Config.FLASH_INTERVAL_MS)
    fun alertText(c: Context) = json(c).optString("alertText", Config.ALERT_TEXT)
    fun settingsVersion(c: Context) = json(c).optInt("version", 0)

    fun vibration(c: Context): LongArray {
        val a = json(c).optJSONArray("vibrationPattern") ?: return Config.VIBRATION_PATTERN
        return if (a.length() < 2) Config.VIBRATION_PATTERN else LongArray(a.length()) { a.getLong(it) }
    }

    /** Timing records so the status screen can show a late alarm. */
    fun recordDue(c: Context, ms: Long) = p(c).edit().putLong("lastDue", ms).apply()
    fun recordFired(c: Context) = p(c).edit().putLong("lastFired", System.currentTimeMillis()).apply()
    fun recordPosted(c: Context) = p(c).edit().putLong("lastPosted", System.currentTimeMillis()).apply()
    fun recordShown(c: Context) = p(c).edit().putLong("lastShown", System.currentTimeMillis()).apply()
    fun lastPosted(c: Context) = p(c).getLong("lastPosted", 0L)
    fun lastShown(c: Context) = p(c).getLong("lastShown", 0L)
    fun lastDue(c: Context) = p(c).getLong("lastDue", 0L)
    fun lastFired(c: Context) = p(c).getLong("lastFired", 0L)

    /** Milliseconds since epoch of the last successful fetch, or 0 if never. */
    fun lastFetch(c: Context) = p(c).getLong("lastFetch", 0L)

    /** Blocking network call: run off the main thread. True if fresh settings were saved. */
    fun fetch(c: Context): Boolean = try {
        val url = URL(Config.SETTINGS_URL + "?t=" + System.currentTimeMillis())
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = Config.FETCH_TIMEOUT_MS
        conn.readTimeout = Config.FETCH_TIMEOUT_MS
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        JSONObject(text)   // reject anything that is not valid JSON
        p(c).edit().putString("remote", text).putLong("lastFetch", System.currentTimeMillis()).apply()
        true
    } catch (_: Exception) {
        false
    }
}
