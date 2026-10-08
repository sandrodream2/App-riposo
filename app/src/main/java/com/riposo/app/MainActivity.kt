package com.riposo.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var startText: TextView
    private lateinit var endText: TextView
    private lateinit var statusText: TextView

    private var startHour = 22
    private var startMinute = 0
    private var endHour = 7
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val schedule = Prefs.getSchedule(this)
        startHour = schedule.startHour
        startMinute = schedule.startMinute
        endHour = schedule.endHour
        endMinute = schedule.endMinute

        startText = findViewById(R.id.startText)
        endText = findViewById(R.id.endText)
        statusText = findViewById(R.id.statusText)

        val recycler: RecyclerView = findViewById(R.id.appRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AppPickerAdapter(loadApps())

        updateLabels()
        refreshStatus()

        findViewById<Button>(R.id.pickStart).setOnClickListener { showTimePicker(true) }
        findViewById<Button>(R.id.pickEnd).setOnClickListener { showTimePicker(false) }
        findViewById<Button>(R.id.toggleSchedule).setOnClickListener { toggleSchedule() }
        findViewById<Button>(R.id.openAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun loadApps(): List<InstalledApp> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        return apps.asSequence()
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { InstalledApp(it.loadLabel(pm).toString(), it.packageName, it.loadIcon(pm)) }
            .filter { it.packageName != packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun showTimePicker(isStart: Boolean) {
        val initial = if (isStart) startHour to startMinute else endHour to endMinute
        val picker = TimePickerFragment(initial.first, initial.second) { h, m ->
            if (isStart) {
                startHour = h; startMinute = m
            } else {
                endHour = h; endMinute = m
            }
            saveSchedule()
            updateLabels()
            refreshStatus()
        }
        picker.show(supportFragmentManager, "time")
    }

    private fun toggleSchedule() {
        val schedule = Prefs.getSchedule(this)
        val newEnabled = !schedule.enabled
        Prefs.setSchedule(this, BlockSchedule(startHour, startMinute, endHour, endMinute, newEnabled))
        refreshStatus()
        val msg = if (newEnabled) R.string.schedule_enabled_toast else R.string.schedule_disabled_toast
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        if (newEnabled) checkAccessibility()
    }

    private fun saveSchedule() {
        val current = Prefs.getSchedule(this)
        Prefs.setSchedule(this, BlockSchedule(startHour, startMinute, endHour, endMinute, current.enabled))
    }

    private fun updateLabels() {
        startText.text = String.format("%02d:%02d", startHour, startMinute)
        endText.text = String.format("%02d:%02d", endHour, endMinute)
    }

    private fun refreshStatus() {
        val schedule = Prefs.getSchedule(this)
        val cal = Calendar.getInstance()
        val active = schedule.enabled && schedule.isActive(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        statusText.text = getString(
            if (schedule.enabled) {
                if (active) R.string.status_active else R.string.status_waiting
            } else R.string.status_off
        )
        val toggle: Button = findViewById(R.id.toggleSchedule)
        toggle.setText(if (schedule.enabled) R.string.disable_schedule else R.string.enable_schedule)
    }

    private fun checkAccessibility() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, R.string.accessibility_needed, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val setting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return !TextUtils.isEmpty(setting) && setting.contains("$packageName/AppBlockService")
    }
}
