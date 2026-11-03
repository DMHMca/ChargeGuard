package uk.mcaughtry.chargeguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()   // lets us fetch settings before deciding
        Thread {
            try {
                if (intent.action == Scheduler.ACTION_CHECK) {
                    Prefs.fetch(context)   // best effort; falls back to the cached copy
                    // The daily alarm re-arms itself for tomorrow, using any new hour.
                    if (intent.getIntExtra("code", Scheduler.DAILY) == Scheduler.DAILY) {
                        Scheduler.scheduleDaily(context)
                    }
                    Alert.checkAndAlert(context)
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
