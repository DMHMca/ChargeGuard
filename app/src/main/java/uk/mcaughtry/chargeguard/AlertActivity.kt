package uk.mcaughtry.chargeguard

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Full-screen flashing alert: alternates red and white at full brightness with a
 * repeating vibration pattern. Stops when the watch is put on a charger, when
 * tapped (then re-checks in 30 minutes), or after 5 minutes.
 */
class AlertActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var label: TextView
    private var red = true
    private var recheckOnExit = true

    private val flash = object : Runnable {
        override fun run() {
            red = !red
            root.setBackgroundColor(if (red) Color.RED else Color.WHITE)
            label.setTextColor(if (red) Color.WHITE else Color.RED)
            handler.postDelayed(this, Prefs.flashIntervalMs(this@AlertActivity))
        }
    }

    private val timeout = Runnable { finish() }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            recheckOnExit = false   // charging: job done
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }

        val pct = intent.getIntExtra("pct", Alert.batteryStatus(this).first)
        label = TextView(this).apply {
            text = "${Prefs.alertText(this@AlertActivity)}\n$pct%"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.RED)
            addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnClickListener { finish() }
        }
        setContentView(root)

        ContextCompat.registerReceiver(
            this, powerReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        startVibrating()
        handler.post(flash)
        handler.postDelayed(timeout, Config.ALERT_TIMEOUT_MINUTES * 60_000L)
    }

    @Suppress("DEPRECATION")
    private fun startVibrating() {
        val effect = VibrationEffect.createWaveform(
            Prefs.vibration(this), 0   // repeat from index 0
        )
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        vibrator().vibrate(effect, attrs)
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31)
            getSystemService(VibratorManager::class.java).defaultVibrator
        else
            getSystemService(Vibrator::class.java)

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        vibrator().cancel()
        try { unregisterReceiver(powerReceiver) } catch (_: IllegalArgumentException) {}
        Alert.clear(this)
        if (recheckOnExit) Scheduler.scheduleRecheck(this)
        super.onDestroy()
    }
}
