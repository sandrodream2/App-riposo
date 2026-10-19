package com.riposo.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var blockedCountText: TextView
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
        blockedCountText = findViewById(R.id.blockedCountText)
        val recycler: RecyclerView = findViewById(R.id.appRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AppPickerAdapter(loadApps()) { updateBlockedCount() }
        recycler.adapter = adapter
        val searchInput = findViewById<android.widget.EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString() ?: "")
            }
        })
        updateLabels()
        refreshStatus()
        updateBlockedCount()
        findViewById<Button>(R.id.pickStart).setOnClickListener { showTimePicker(true) }
        findViewById<Button>(R.id.pickEnd).setOnClickListener { showTimePicker(false) }
        findViewById<Button>(R.id.toggleSchedule).setOnClickListener { toggleSchedule() }
        findViewById<Button>(R.id.blockNow15).setOnClickListener { startImmediateBlock(15) }
        findViewById<Button>(R.id.blockNow30).setOnClickListener { startImmediateBlock(30) }
        findViewById<Button>(R.id.blockNow60).setOnClickListener { startImmediateBlock(60) }
        findViewById<Button>(R.id.selectAllButton).setOnClickListener {
            Prefs.setBlockedApps(this, adapter.allApps().map { it.packageName }.toSet())
            adapter.refreshChecks()
            updateBlockedCount()
        }
        findViewById<Button>(R.id.clearAllButton).setOnClickListener {
            Prefs.setBlockedApps(this, emptySet())
            adapter.refreshChecks()
            updateBlockedCount()
        }
        val dumbphoneSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.dumbphoneSwitch)
        dumbphoneSwitch.isChecked = Prefs.isDumbphoneEnabled(this)
        dumbphoneSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setDumbphoneEnabled(this, checked)
            val msg = if (checked) R.string.dumbphone_enabled_toast else R.string.dumbphone_disabled_toast
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (checked) checkAccessibility()
        }
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

    private fun startImmediateBlock(minutes: Int) {
        Prefs.setImmediateBlockEnd(this, System.currentTimeMillis() + minutes * 60_000L)
        Prefs.setImmediateBlockPackages(this, Prefs.getBlockedApps(this))
        Toast.makeText(this, getString(R.string.block_now_toast, "$minutes"), Toast.LENGTH_SHORT).show()
        checkAccessibility()
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
                if (active) R.string.status_active_until else R.string.status_waiting
            } else R.string.status_off,
            String.format("%02d:%02d", schedule.endHour, schedule.endMinute)
        )
        findViewById<Button>(R.id.toggleSchedule).setText(
            if (schedule.enabled) R.string.disable_schedule else R.string.enable_schedule
        )
    }

    private fun updateBlockedCount() {
        val count = Prefs.getBlockedApps(this).size
        blockedCountText.text = getString(R.string.blocked_count, count)
    }

    private fun checkAccessibility() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, R.string.accessibility_needed, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val setting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return !setting.isNullOrEmpty() && setting.contains("$packageName/AppBlockService")
    }
}
