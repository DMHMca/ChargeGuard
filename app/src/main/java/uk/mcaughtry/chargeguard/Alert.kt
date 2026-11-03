package uk.mcaughtry.chargeguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.app.NotificationCompat

object Alert {
    private const val CHANNEL = "charge_alert"
    private const val NOTIFICATION_ID = 1

    /** Returns battery percentage and whether the watch is on a charger. */
    fun batteryStatus(c: Context): Pair<Int, Boolean> {
        val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val plugged = (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val pct = if (level < 0 || scale <= 0) 100 else level * 100 / scale
        return pct to plugged
    }

    fun checkAndAlert(c: Context) {
        val (pct, plugged) = batteryStatus(c)
        if (plugged || pct > Prefs.threshold(c)) return
        show(c, pct)
    }

    /**
     * Posts a full-screen-intent notification. That is what lets the alert take
     * over the screen from the background; the notification's own vibration is
     * the fallback if the system refuses the full-screen part.
     */
    fun show(c: Context, pct: Int) {
        Prefs.recordPosted(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, "Charge alert", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = Config.VIBRATION_PATTERN
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)

        val pi = PendingIntent.getActivity(
            c, 0,
            Intent(c, AlertActivity::class.java)
                .putExtra("pct", pct)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("Charge your watch")
            .setContentText("Battery $pct%")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, n)

        // Also launch the flashing screen directly. From the background this only works
        // if "display over other apps" is allowed, but then it works even when the screen
        // is on, which the full-screen notification above does not. If it is refused, the
        // notification remains the fallback.
        try {
            c.startActivity(
                Intent(c, AlertActivity::class.java)
                    .putExtra("pct", pct)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } catch (_: Exception) {
        }
    }

    fun clear(c: Context) {
        c.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
