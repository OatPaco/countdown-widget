package com.oat.countdown

import android.content.Context

object PrefsHelper {
    private const val PREFS_NAME = "countdown_widget_prefs"

    fun getSelectedIds(context: Context, widgetId: Int): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("ids_$widgetId", emptySet()) ?: emptySet()
    }

    fun setSelectedIds(context: Context, widgetId: Int, ids: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet("ids_$widgetId", ids)
            .apply()
    }

    fun clear(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("ids_$widgetId")
            .apply()
    }
}
