package com.riposo.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.riposo.app.databinding.ActivityBlockBinding
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BlockActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    private lateinit var binding: ActivityBlockBinding
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateCountdown()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.homeButton.setOnClickListener {
            BlockState.setBlocking(this, false)
            finish()
        }
        updateCountdown()
    }

    override fun onResume() {
        super.onResume()
        if (!stillBlocked()) {
            BlockState.setBlocking(this, false)
            finish()
            return
        }
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun stillBlocked(): Boolean {
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return false
        val cal = Calendar.getInstance()
        val schedule = Prefs.getSchedule(this)
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (Prefs.isImmediateBlockActive(this) &&
            Prefs.getImmediateBlockPackages(this).contains(pkg)
        ) return true
        if (Prefs.isDumbphoneActive(this) && !Prefs.allowedDuringDumbphone(this, pkg)) return true
        return schedule.enabled && schedule.isActive(
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
        ) && Prefs.getBlockedApps(this).contains(pkg)
    }

    private fun blockEndMillis(): Long {
        if (Prefs.isImmediateBlockActive(this)) return Prefs.getImmediateBlockEnd(this)
        val schedule = Prefs.getSchedule(this)
        val cal = Calendar.getInstance()
        val endMin = schedule.endHour * 60 + schedule.endMinute
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val end = cal.clone() as Calendar
        if (endMin <= nowMin) end.add(Calendar.DAY_OF_YEAR, 1)
        end.set(Calendar.HOUR_OF_DAY, schedule.endHour)
        end.set(Calendar.MINUTE, schedule.endMinute)
        end.set(Calendar.SECOND, 0)
        return end.timeInMillis
    }

    private fun updateCountdown() {
        val endMillis = blockEndMillis()
        val remaining = endMillis - System.currentTimeMillis()
        val isDumbphone = Prefs.isDumbphoneActive(this) &&
            !Prefs.allowedDuringDumbphone(this, intent.getStringExtra(EXTRA_PACKAGE) ?: "")
        val endText = String.format(
            "%02d:%02d",
            Prefs.getSchedule(this).endHour, Prefs.getSchedule(this).endMinute
        )
        binding.messageText.text = getString(
            if (isDumbphone) R.string.block_message_dumbphone else R.string.block_message,
            endText
        )
        if (remaining > 0) {
            val h = TimeUnit.MILLISECONDS.toHours(remaining)
            val m = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
            binding.countdownText.text = String.format("%02d:%02d:%02d", h, m, s)
        } else {
            binding.countdownText.text = getString(R.string.countdown_label)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
