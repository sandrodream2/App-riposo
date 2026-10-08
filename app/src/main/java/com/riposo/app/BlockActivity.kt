package com.riposo.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.riposo.app.databinding.ActivityBlockBinding
import java.util.Calendar

class BlockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    private lateinit var binding: ActivityBlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val schedule = Prefs.getSchedule(this)
        val endText = String.format("%02d:%02d", schedule.endHour, schedule.endMinute)
        binding.messageText.text = getString(R.string.block_message, endText)

        binding.homeButton.setOnClickListener {
            BlockState.setBlocking(this, false)
            finish()
        }
    }

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
}
