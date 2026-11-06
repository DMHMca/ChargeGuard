package uk.mcaughtry.chargeguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat

object Alert {
    private const val CHANNEL = "charge_alert"
    private const val NOTIFICATION_ID = 1
    private const val CLEAR_JOB_ID = 2

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
        // Without the full-screen and overlay permissions (the normal case for an app installed
        // from Google Play) Android will not let the flashing screen start by itself. Then the
        // notification has to do the work: wake the screen, repeat the vibration until it is
        // dismissed, and disappear after the usual timeout or when the charger goes on.
        val canTakeOver = Status.fullScreenAllowed(c) || Status.overlayAllowed(c)
        val builder = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("Charge your watch")
            .setContentText("Battery $pct%")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
        if (!canTakeOver) builder.setTimeoutAfter(Config.ALERT_TIMEOUT_MINUTES * 60_000L)
        val n = builder.build()
        if (!canTakeOver) n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify(NOTIFICATION_ID, n)
        if (!canTakeOver) {
            wakeScreen(c)
            clearWhenCharging(c)
        }

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

    /** Turns the screen on for a few seconds so the notification is seen. Needs no special permission. */
    @Suppress("DEPRECATION")
    private fun wakeScreen(c: Context) {
        try {
            c.getSystemService(PowerManager::class.java)
                .newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "ChargeGuard:alert"
                ).acquire(10_000L)
        } catch (_: Exception) {
        }
    }

    /** Asks Android to run ClearOnChargeJob once the watch is on a charger, which removes the alert. */
    private fun clearWhenCharging(c: Context) {
        try {
            c.getSystemService(JobScheduler::class.java).schedule(
                JobInfo.Builder(CLEAR_JOB_ID, ComponentName(c, ClearOnChargeJob::class.java))
                    .setRequiresCharging(true)
                    .build()
            )
        } catch (_: Exception) {
        }
    }

    fun clear(c: Context) {
        c.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
