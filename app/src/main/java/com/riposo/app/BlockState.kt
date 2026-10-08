package com.riposo.app

import android.content.Context
import android.content.SharedPreferences

object BlockState {
    private const val FILE = "block_state"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isBlocking(ctx: Context): Boolean = prefs(ctx).getBoolean("blocking", false)

    fun setBlocking(ctx: Context, value: Boolean) =
        prefs(ctx).edit().putBoolean("blocking", value).apply()
}
