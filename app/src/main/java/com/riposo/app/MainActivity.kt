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
    private lateinit var appsCountText: TextView
    private lateinit var accessibilityStatus: TextView
    private lateinit var adapter: AppPickerAdapter

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
        appsCountText = findViewById(R.id.appsCountText)
        accessibilityStatus = findViewById(R.id.accessibilityStatus)

        adapter = AppPickerAdapter(loadApps(), ::onBlockedAppsChanged)
        adapter.setInitialBlocked(Prefs.getBlockedApps(this))
        val recycler: RecyclerView = findViewById(R.id.appRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<TextView>(R.id.motivationText).text = todayMotivation()

        updateLabels()
        refreshStatus()

        findViewById<Button>(R.id.pickStart).setOnClickListener { showTimePicker(true) }
        findViewById<Button>(R.id.pickEnd).setOnClickListener { showTimePicker(false) }
        findViewById<Button>(R.id.toggleSchedule).setOnClickListener { toggleSchedule() }
        findViewById<Button>(R.id.presetNight).setOnClickListener { applyPreset(22, 0, 7, 0) }
        findViewById<Button>(R.id.presetStudy).setOnClickListener {
            val cal = Calendar.getInstance()
            applyPreset(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                (cal.get(Calendar.HOUR_OF_DAY) + 2) % 24, cal.get(Calendar.MINUTE))
        }
        findViewById<Button>(R.id.presetDinner).setOnClickListener { applyPreset(20, 0, 22, 0) }
        findViewById<Button>(R.id.openAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun onBlockedAppsChanged(count: Int) {
        appsCountText.text = getString(R.string.apps_selected, count)
    }

    private fun todayMotivation(): String {
        val array = resources.getStringArray(R.array.motivations)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return array[dayOfYear % array.size]
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

    private fun applyPreset(sh: Int, sm: Int, eh: Int, em: Int) {
        startHour = sh; startMinute = sm; endHour = eh; endMinute = em
        val current = Prefs.getSchedule(this)
        Prefs.setSchedule(this, BlockSchedule(sh, sm, eh, em, current.enabled))
        updateLabels()
        refreshStatus()
        if (!current.enabled) toggleSchedule()
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
        statusText.text = when {
            !schedule.enabled -> getString(R.string.status_off)
            active -> getString(R.string.status_active) + " — " +
                getString(R.string.status_until, String.format("%02d:%02d", endHour, endMinute))
            else -> getString(R.string.status_waiting)
        }
        val toggle: Button = findViewById(R.id.toggleSchedule)
        toggle.setText(if (schedule.enabled) R.string.disable_schedule else R.string.enable_schedule)
        appsCountText.text = getString(R.string.apps_selected, Prefs.getBlockedApps(this).size)
        accessibilityStatus.text =
            if (isAccessibilityEnabled()) getString(R.string.accessibility_ok)
            else getString(R.string.open_accessibility)
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
