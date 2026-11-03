package uk.mcaughtry.chargeguard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Uses setAlarmClock, which is exact, survives Doze, and needs no special
 * "exact alarm" permission. The trade-off is a small alarm icon on the watch.
 */
object Scheduler {
    const val ACTION_CHECK = "uk.mcaughtry.chargeguard.CHECK"
    const val DAILY = 1
    const val RECHECK = 2

    fun scheduleDaily(c: Context) {
        if (!Prefs.enabled(c)) {
            cancel(c, DAILY)
            cancel(c, RECHECK)
            return
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Prefs.hour(c))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        setAt(c, cal.timeInMillis, DAILY)
    }

    /** Check again later if the alert was dismissed without charging. Stops at 11pm. */
    fun scheduleRecheck(c: Context, delayMinutes: Int = Prefs.recheckMinutes(c)) {
        if (!Prefs.enabled(c)) return
        val t = System.currentTimeMillis() + delayMinutes * 60_000L
        val hour = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.HOUR_OF_DAY)
        if (hour >= Config.LAST_RECHECK_HOUR || hour < Prefs.hour(c)) return
        setAt(c, t, RECHECK)
    }

    private fun intentFor(c: Context, code: Int) =
        Intent(c, AlarmReceiver::class.java).setAction(ACTION_CHECK).putExtra("code", code)

    private fun setAt(c: Context, millis: Long, code: Int) {
        val am = c.getSystemService(AlarmManager::class.java)
        val trigger = PendingIntent.getBroadcast(
            c, code, intentFor(c, code),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), trigger)
    }

    private fun cancel(c: Context, code: Int) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            c, code, intentFor(c, code),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}
