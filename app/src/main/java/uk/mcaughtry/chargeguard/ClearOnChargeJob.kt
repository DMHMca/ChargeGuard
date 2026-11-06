package uk.mcaughtry.chargeguard

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * Runs once the watch is on a charger (see Alert.clearWhenCharging) and removes the alert
 * notification, which also stops its repeating vibration. It is the "stops when you put the
 * watch on the charger" behaviour for the no-permission alert, which has no screen of its own.
 */
class ClearOnChargeJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Alert.clear(this)
        return false   // finished; nothing running in the background
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
