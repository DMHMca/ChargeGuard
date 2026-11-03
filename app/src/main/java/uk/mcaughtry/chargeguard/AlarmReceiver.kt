package uk.mcaughtry.chargeguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                if (intent.action == Scheduler.ACTION_CHECK) {
                    Prefs.recordFired(context)
                    val code = intent.getIntExtra("code", Scheduler.DAILY)
                    val daily = code == Scheduler.DAILY
                    // 1. Re-arm tomorrow's alarm first, using the saved hour.
                    if (daily) Scheduler.scheduleDaily(context, afterToday = true)
                    // 2. Alert straight away from the saved settings. Never wait on the network.
                    if (code == Scheduler.TEST) Alert.show(context, Alert.batteryStatus(context).first)  // a test always fires
                    else Alert.checkAndAlert(context)
                    // 3. Only then fetch fresh settings for next time, and re-arm in case the hour changed.
                    if (Prefs.fetch(context) && daily) Scheduler.scheduleDaily(context, afterToday = true)
                } else {
                    // Boot, app update, timezone change: just re-arm the daily alarm.
                    Scheduler.scheduleDaily(context)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
