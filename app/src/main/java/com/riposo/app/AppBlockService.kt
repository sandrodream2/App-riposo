package com.riposo.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

class AppBlockService : AccessibilityService() {

    companion object {
        const val ACTION_CHECK = "com.riposo.app.CHECK"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_DEFAULT
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        checkPackage(pkg)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    private fun checkPackage(pkg: String) {
        if (!BlockState.isBlocking(this)) {
            if (Prefs.getBlockedApps(this).contains(pkg) && isScheduleActive()) {
                BlockState.setBlocking(this, true)
                launchBlock(pkg)
            }
            return
        }
        if (Prefs.getBlockedApps(this).contains(pkg) && isScheduleActive()) {
            launchBlock(pkg)
        } else {
            BlockState.setBlocking(this, false)
        }
    }

    private fun isScheduleActive(): Boolean {
        val schedule = Prefs.getSchedule(this)
        if (!schedule.enabled) return false
        val cal = Calendar.getInstance()
        return schedule.isActive(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun launchBlock(pkg: String) {
        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(BlockActivity.EXTRA_PACKAGE, pkg)
        }
        startActivity(intent)
    }
}
