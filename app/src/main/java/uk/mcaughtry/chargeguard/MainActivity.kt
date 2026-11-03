package uk.mcaughtry.chargeguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

/**
 * Status screen. Settings are not edited here: change config/settings.json on
 * GitHub, then press "Refresh settings" (or wait for the next evening check).
 */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var levelBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        val pad = (resources.displayMetrics.density * 28).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { gravity = Gravity.CENTER; textSize = 14f }
        val refreshBtn = Button(this).apply {
            text = "Refresh settings"
            setOnClickListener {
                text = "Fetching..."
                Thread {
                    val ok = Prefs.fetch(context)
                    Scheduler.scheduleDaily(context)
                    runOnUiThread {
                        text = if (ok) "Refresh settings" else "No connection; retry"
                        refresh()
                    }
                }.start()
            }
        }
        val testBtn = Button(this).apply {
            text = "Test alert"
            setOnClickListener { startActivity(Intent(context, AlertActivity::class.java)) }
        }
        levelBtn = Button(this).apply {
            setOnClickListener {
                // Cycle the test override: 50, 55 ... 100, then back to the settings file.
                val cur = Prefs.overrideThreshold(context)
                Prefs.setOverrideThreshold(context, if (cur < 0) 50 else if (cur >= 100) -1 else cur + 5)
                refresh()
            }
        }
        val timedBtn = Button(this).apply {
            text = "Test in 1 minute"
            setOnClickListener {
                Scheduler.scheduleTest(context)
                text = "Due in 1 min: wrist down"
            }
        }
        listOf(status, refreshBtn, levelBtn, timedBtn, testBtn).forEach { column.addView(it) }
        setContentView(ScrollView(this).apply { addView(column) })

        Scheduler.scheduleDaily(this)
        refresh()
        // Pick up any new settings on every open.
        Thread { if (Prefs.fetch(this)) { Scheduler.scheduleDaily(this); runOnUiThread { refresh() } } }.start()
    }

    private fun refresh() {
        val (pct, plugged) = Alert.batteryStatus(this)
        val override = Prefs.overrideThreshold(this)
        levelBtn.text = if (override >= 0) "Alert at $override% (TEST, tap)"
        else "Alert at ${Prefs.settingsThreshold(this)}% (settings)"
        val info = packageManager.getPackageInfo(packageName, 0)
        val last = Prefs.lastFetch(this)
        val fetched = if (last == 0L) "never fetched"
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(last))
        status.text = "Battery $pct%" + (if (plugged) " (charging)" else "") + "\n" +
            (if (Prefs.enabled(this)) "Armed: %02d:00, at %d%% or less%s".format(Prefs.hour(this), Prefs.threshold(this), if (override >= 0) " (TEST)" else "")
            else "Off") + "\n" +
            "Settings v${Prefs.settingsVersion(this)}\n$fetched\n" +
            "App v${info.versionName} (${info.longVersionCode})"
    }
}
