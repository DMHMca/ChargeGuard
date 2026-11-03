package uk.mcaughtry.chargeguard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The technical details, kept off the main screen. */
class InfoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 30).toInt()
        val details = TextView(this).apply {
            text = Status.details(this@InfoActivity)
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        val close = Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            addView(details)
            addView(close)
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }
}
