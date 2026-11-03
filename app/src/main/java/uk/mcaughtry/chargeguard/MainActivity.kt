package uk.mcaughtry.chargeguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Main screen: battery and one status line at the top, then the main buttons.
 * Technical details live behind the Info button (InfoActivity).
 */
class MainActivity : Activity() {
    private lateinit var battery: TextView
    private lateinit var armed: TextView
    private lateinit var levelBtn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun styledButton(label: String, color: Int, onClick: (Button) -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            minHeight = dp(44)
            minimumHeight = dp(44)
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(24).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(4), 0, dp(4)) }
            setOnClickListener { onClick(this) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        val grey = Color.parseColor("#3A3A3C")
        val blue = Color.parseColor("#2F6FDB")

        battery = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        }
        armed = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }

        val timedBtn = styledButton("Test in 1 minute", blue) { btn ->
            Scheduler.scheduleTest(this)
            buzz()
            Toast.makeText(this, "Test set. Alert in 1 minute: let the screen go dark.", Toast.LENGTH_LONG).show()
            object : CountDownTimer(60_000, 1000) {
                override fun onTick(msLeft: Long) { btn.text = "Alert in ${msLeft / 1000 + 1}s" }
                override fun onFinish() { btn.text = "Test in 1 minute" }
            }.start()
        }
        val testBtn = styledButton("Test alert now", grey) {
            startActivity(Intent(this, AlertActivity::class.java))
        }
        levelBtn = styledButton("", grey) { pickLevel() }
        val refreshBtn = styledButton("Refresh settings", grey) { btn ->
            btn.text = "Fetching..."
            Thread {
                val ok = Prefs.fetch(this)
                Scheduler.scheduleDaily(this)
                runOnUiThread {
                    btn.text = "Refresh settings"
                    buzz()
                    Toast.makeText(
                        this,
                        if (ok) "Settings updated: v${Prefs.settingsVersion(this)}"
                        else "Couldn't reach GitHub. Check the watch's Wi-Fi.",
                        Toast.LENGTH_LONG
                    ).show()
                    refresh()
                }
            }.start()
        }
        val infoBtn = styledButton("Info", grey) { startActivity(Intent(this, InfoActivity::class.java)) }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(36), dp(22), dp(40))
            addView(battery)
            addView(armed)
            addView(timedBtn)
            addView(testBtn)
            addView(levelBtn)
            addView(refreshBtn)
            addView(infoBtn)
        }
        setContentView(ScrollView(this).apply { addView(column) })

        Scheduler.scheduleDaily(this)
        refresh()
        askForFullScreen()
        askForOverlay()
        // Pick up any new settings on every open.
        Thread {
            if (Prefs.fetch(this)) {
                Scheduler.scheduleDaily(this)
                runOnUiThread { refresh() }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** A swipe wheel (steps of 5) that sets the alert level on the watch itself. */
    private fun pickLevel() {
        val labels = Array(20) { ((it + 1) * 5).toString() }
        val current = Prefs.threshold(this).coerceIn(5, 100)
        val picker = NumberPicker(this).apply {
            minValue = 0
            maxValue = labels.size - 1
            displayedValues = labels
            value = (Math.round(current / 5f) - 1).coerceIn(0, labels.size - 1)
            wrapSelectorWheel = false
        }
        AlertDialog.Builder(this)
            .setTitle("Alert at (%)")
            .setView(picker)
            .setPositiveButton("Set") { _, _ -> Prefs.setWatchThreshold(this, (picker.value + 1) * 5); refresh() }
            .setNeutralButton("Use file") { _, _ -> Prefs.setWatchThreshold(this, -1); refresh() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** A short buzz so a button press visibly did something. */
    private fun buzz() {
        val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else getSystemService(Vibrator::class.java)
        v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** "Display over other apps" lets the alert take over even with the screen on. Asked once. */
    private fun askForOverlay() {
        if (Status.overlayAllowed(this)) return
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (prefs.getBoolean("askedOverlay", false)) return
        prefs.edit().putBoolean("askedOverlay", true).apply()
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            // No such screen on this watch; grant with adb instead (see CLAUDE.md).
        }
    }

    /** Opens the full-screen permission screen once if it is not allowed. */
    private fun askForFullScreen() {
        if (Status.fullScreenAllowed(this)) return
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (prefs.getBoolean("askedFullScreen", false)) return
        prefs.edit().putBoolean("askedFullScreen", true).apply()
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            // This watch has no such screen; the Info screen will say so.
        }
    }

    private fun refresh() {
        val (pct, plugged) = Alert.batteryStatus(this)
        battery.text = if (plugged) "$pct% charging" else "$pct%"
        armed.text = Status.armedLine(this)
        val onWatch = Prefs.watchThreshold(this)
        levelBtn.text = if (onWatch >= 0) "Alert level: $onWatch%"
        else "Alert level: ${Prefs.fileThreshold(this)}% (file)"
    }
}
