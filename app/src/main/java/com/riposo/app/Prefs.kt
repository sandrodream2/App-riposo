package com.riposo.app

import android.content.Context
import android.content.SharedPreferences

data class BlockSchedule(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val enabled: Boolean
) {
    fun isActive(hour: Int, minute: Int): Boolean {
        val now = hour * 60 + minute
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        return if (start <= end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }
}

object Prefs {
    private const val FILE = "riposo_prefs"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getBlockedApps(ctx: Context): MutableSet<String> =
        prefs(ctx).getStringSet("blocked_apps", emptySet())!!.toMutableSet()

    fun setBlockedApps(ctx: Context, apps: Set<String>) =
        prefs(ctx).edit().putStringSet("blocked_apps", apps).apply()

    fun getSchedule(ctx: Context): BlockSchedule {
        val p = prefs(ctx)
        return BlockSchedule(
            p.getInt("start_hour", 22), p.getInt("start_minute", 0),
            p.getInt("end_hour", 7), p.getInt("end_minute", 0),
            p.getBoolean("schedule_enabled", false)
        )
    }

    fun setSchedule(ctx: Context, s: BlockSchedule) {
        prefs(ctx).edit()
            .putInt("start_hour", s.startHour)
            .putInt("start_minute", s.startMinute)
            .putInt("end_hour", s.endHour)
            .putInt("end_minute", s.endMinute)
            .putBoolean("schedule_enabled", s.enabled)
            .apply()
    }

    fun getIgnoreBatteryOptimizationHintShown(ctx: Context): Boolean =
        prefs(ctx).getBoolean("battery_hint_shown", false)

    fun setIgnoreBatteryOptimizationHintShown(ctx: Context) =
        prefs(ctx).edit().putBoolean("battery_hint_shown", true).apply()
}
