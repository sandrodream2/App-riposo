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
    private const val DEFAULT_HOME = "com.android.dialer"
    private const val DEFAULT_MESSAGES = "com.google.android.apps.messaging"
    private const val DEFAULT_CLOCK = "com.google.android.deskclock"
    private const val DEFAULT_SETTINGS = "com.android.settings"

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

    fun isDumbphoneEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean("dumbphone_enabled", false)

    fun setDumbphoneEnabled(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean("dumbphone_enabled", value).apply()

    fun getImmediateBlockEnd(ctx: Context): Long =
        prefs(ctx).getLong("immediate_block_end", 0L)

    fun setImmediateBlockEnd(ctx: Context, endMillis: Long) =
        prefs(ctx).edit().putLong("immediate_block_end", endMillis).apply()

    fun getImmediateBlockPackages(ctx: Context): Set<String> =
        prefs(ctx).getStringSet("immediate_block_packages", emptySet())!!

    fun setImmediateBlockPackages(ctx: Context, packages: Set<String>) =
        prefs(ctx).edit().putStringSet("immediate_block_packages", packages).apply()

    fun isDumbphoneActive(ctx: Context): Boolean {
        if (!isDumbphoneEnabled(ctx)) return false
        val schedule = getSchedule(ctx)
        val cal = java.util.Calendar.getInstance()
        return schedule.isActive(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }

    fun isImmediateBlockActive(ctx: Context): Boolean =
        System.currentTimeMillis() < getImmediateBlockEnd(ctx)

    fun allowedDuringDumbphone(ctx: Context, pkg: String): Boolean {
        val allowed = setOf(
            DEFAULT_HOME.takeIf { it.isNotEmpty() },
            "com.android.phone",
            "com.android.contacts",
            "com.android.dialer",
            DEFAULT_MESSAGES,
            "com.android.mms",
            DEFAULT_CLOCK,
            "com.android.deskclock",
            "com.sec.android.app.clockpackage",
            DEFAULT_SETTINGS,
            "com.android.settings",
            ctx.packageName
        )
        return allowed.contains(pkg) || pkg.startsWith("com.android.")
    }
}
