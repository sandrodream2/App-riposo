package com.riposo.app

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import kotlin.concurrent.thread

class BlockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    private var countdownTimer: CountDownTimer? = null
    private var breathing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        val schedule = Prefs.getSchedule(this)
        val endText = String.format("%02d:%02d", schedule.endHour, schedule.endMinute)
        findViewById<TextView>(R.id.messageText).text = getString(R.string.block_message, endText)

        findViewById<Button>(R.id.homeButton).setOnClickListener {
            BlockState.setBlocking(this, false)
            finish()
        }

        findViewById<Button>(R.id.breathingButton).setOnClickListener { startBreathing() }

        startCountdown()
    }

    private fun startCountdown() {
        val schedule = Prefs.getSchedule(this)
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(millisUntilEnd(schedule), 60_000L) {
            override fun onTick(millis: Long) {
                findViewById<TextView>(R.id.countdownText).text =
                    getString(R.string.block_countdown, formatRemaining(millis))
            }

            override fun onFinish() {
                BlockState.setBlocking(this@BlockActivity, false)
                finish()
            }
        }.start()
        findViewById<TextView>(R.id.countdownText).text =
            getString(R.string.block_countdown, formatRemaining(millisUntilEnd(schedule)))
    }

    private fun millisUntilEnd(schedule: BlockSchedule): Long {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, schedule.endHour)
        cal.set(Calendar.MINUTE, schedule.endMinute)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis - now
    }

    private fun formatRemaining(millis: Long): String {
        val totalMinutes = (millis / 60_000L).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}h ${String.format("%02d", m)}m" else "${m}m"
    }

    private fun startBreathing() {
        if (breathing) return
        breathing = true
        val text = findViewById<TextView>(R.id.breathingText)
        text.visibility = android.view.View.VISIBLE
        thread {
            repeat(3) {
                runOnUiThread { text.text = getString(R.string.breathing_in) }
                Thread.sleep(4000)
                runOnUiThread { text.text = getString(R.string.breathing_out) }
                Thread.sleep(4000)
            }
            runOnUiThread {
                text.visibility = android.view.View.GONE
                breathing = false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onResume() {
        super.onResume()
        val schedule = Prefs.getSchedule(this)
        val cal = Calendar.getInstance()
        if (!schedule.enabled || !schedule.isActive(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))) {
            BlockState.setBlocking(this, false)
            finish()
        }
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }
}
