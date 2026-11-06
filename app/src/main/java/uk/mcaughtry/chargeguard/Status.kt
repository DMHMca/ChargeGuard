package uk.mcaughtry.chargeguard

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.DateFormat
import java.util.Date

/** Read-only status helpers shared by the main screen and the Info screen. */
object Status {
    fun fullScreenAllowed(c: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun overlayAllowed(c: Context): Boolean = Settings.canDrawOverlays(c)

    private fun timeText(ms: Long) =
        if (ms == 0L) "-" else DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(ms))

    /** One short line for the main screen. */
    fun armedLine(c: Context): String =
        if (Prefs.enabled(c)) "Armed %02d:00 · alert at %d%%".format(Prefs.hour(c), Prefs.threshold(c))
        else "Alerts off"

    /** Everything technical, for the Info screen. */
    fun details(c: Context): String {
        val (pct, plugged) = Alert.batteryStatus(c)
        val info = c.packageManager.getPackageInfo(c.packageName, 0)
        val last = Prefs.lastFetch(c)
        val fetched = if (last == 0L) "never fetched"
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(last))
        val onWatch = Prefs.watchThreshold(c)
        return "ALERT\n" +
            "Battery $pct%" + (if (plugged) " (charging)" else "") + "\n" +
            armedLine(c) + "\n" +
            (if (onWatch >= 0) "Level set on watch" else "Level from settings file") + "\n\n" +
            "PERMISSIONS\n" +
            "Takeover: " + (if (fullScreenAllowed(c)) "allowed" else "NOT allowed") + "\n" +
            "Over apps: " + (if (overlayAllowed(c)) "allowed" else "NOT allowed") + "\n" +
            "Alert style: " + (if (fullScreenAllowed(c) || overlayAllowed(c)) "full flash" else "buzz + notification") + "\n\n" +
            "LAST ALARM\n" +
            "Due ${timeText(Prefs.lastDue(c))}\nFired ${timeText(Prefs.lastFired(c))}\n" +
            "Posted ${timeText(Prefs.lastPosted(c))}\nShown ${timeText(Prefs.lastShown(c))}\n\n" +
            "SETTINGS\n" +
            "v${Prefs.settingsVersion(c)}\n$fetched\n\n" +
            "APP\n" +
            "v${info.versionName} (${info.longVersionCode})"
    }
}
