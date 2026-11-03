package uk.mcaughtry.chargeguard

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

/**
 * Status screen. Settings are not edited here: change config/settings.json on
 * GitHub, then press "Refresh settings" (or wait for the next evening check).
 */
class MainActivity : Activity() {
    private lateinit var status: TextView

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
                        text = "Refresh settings"
                        buzz()
                        Toast.makeText(
                            context,
                            if (ok) "Settings updated: v${Prefs.settingsVersion(context)}"
                            else "Couldn't reach GitHub. Check the watch's Wi-Fi.",
                            Toast.LENGTH_LONG
                        ).show()
                        refresh()
                    }
                }.start()
            }
        }
        val testBtn = Button(this).apply {
            text = "Test alert"
            setOnClickListener { startActivity(Intent(context, AlertActivity::class.java)) }
        }
        val timedBtn = Button(this).apply {
            text = "Test in 1 minute"
            setOnClickListener {
                Scheduler.scheduleTest(context)
                buzz()
                Toast.makeText(context, "Test set. Alert in 1 minute: let the screen go dark.", Toast.LENGTH_LONG).show()
                val btn = this@apply
                object : CountDownTimer(60_000, 1000) {
                    override fun onTick(msLeft: Long) { btn.text = "Alert in ${msLeft / 1000 + 1}s: screen off" }
                    override fun onFinish() { btn.text = "Test in 1 minute" }
                }.start()
            }
        }
        listOf(status, refreshBtn, timedBtn, testBtn).forEach { column.addView(it) }
        setContentView(ScrollView(this).apply { addView(column) })

        Scheduler.scheduleDaily(this)
        refresh()
        askForFullScreen()
        askForOverlay()
        // Pick up any new settings on every open.
        Thread { if (Prefs.fetch(this)) { Scheduler.scheduleDaily(this); runOnUiThread { refresh() } } }.start()
    }

    /** A short buzz so a button press visibly did something. */
    private fun buzz() {
        val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else getSystemService(Vibrator::class.java)
        v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** Android 14+ must allow full-screen notifications for the alert to take over a dark screen. */
    private fun fullScreenAllowed(): Boolean =
        Build.VERSION.SDK_INT < 34 || getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    private fun overlayAllowed(): Boolean = Settings.canDrawOverlays(this)

    /** "Display over other apps" lets the alert take over even with the screen on. Asked once. */
    private fun askForOverlay() {
        if (overlayAllowed()) return
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (prefs.getBoolean("askedOverlay", false)) return
        prefs.edit().putBoolean("askedOverlay", true).apply()
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            // No such screen on this watch; grant with adb instead (see CLAUDE.md).
        }
    }

    /** Opens the permission screen once if it is not allowed. */
    private fun askForFullScreen() {
        if (fullScreenAllowed()) return
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (prefs.getBoolean("askedFullScreen", false)) return
        prefs.edit().putBoolean("askedFullScreen", true).apply()
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            // This watch has no such screen; the status line will say so.
        }
    }

    private fun timeText(ms: Long) =
        if (ms == 0L) "-" else DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(ms))

    private fun refresh() {
        val (pct, plugged) = Alert.batteryStatus(this)
        val info = packageManager.getPackageInfo(packageName, 0)
        val last = Prefs.lastFetch(this)
        val fetched = if (last == 0L) "never fetched"
        else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(last))
        status.text = "Battery $pct%" + (if (plugged) " (charging)" else "") + "\n" +
            (if (Prefs.enabled(this)) "Armed: %02d:00, at %d%% or less".format(Prefs.hour(this), Prefs.threshold(this))
            else "Off") + "\n" +
            "Takeover: " + (if (fullScreenAllowed()) "allowed" else "NOT allowed") + "\n" +
            "Over apps: " + (if (overlayAllowed()) "allowed" else "NOT allowed") + "\n" +
            "Due ${timeText(Prefs.lastDue(this))}\nFired ${timeText(Prefs.lastFired(this))}\n" +
            "Posted ${timeText(Prefs.lastPosted(this))}\nShown ${timeText(Prefs.lastShown(this))}\n" +
            "Settings v${Prefs.settingsVersion(this)}\n$fetched\n" +
            "App v${info.versionName} (${info.longVersionCode})"
    }
}
