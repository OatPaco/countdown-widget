package com.oat.countdown

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class CountdownService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountdownFactory(applicationContext, intent)
    }
}

class CountdownFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    private var events = listOf<CountdownEvent>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Clear calling identity so we can access CalendarContract
        val token = Binder.clearCallingIdentity()
        try {
            val selectedIds = PrefsHelper.getSelectedIds(context, widgetId)
            if (selectedIds.isEmpty()) {
                events = emptyList()
                return
            }
            val allEvents = CalendarHelper.getEvents(context, 1825) // 5 years
            events = allEvents
                .filter { it.id in selectedIds }
                .sortedBy { it.startTime }
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        val ev = events[position]
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            setTextViewText(R.id.emoji, ev.emoji)
            setTextViewText(R.id.title, ev.title)
            setTextViewText(R.id.date_text, ev.dateString)
            setTextViewText(R.id.days, ev.daysLeft.toString())
            setTextViewText(R.id.unit, if (ev.daysLeft == 1) "day" else "days")
            setInt(R.id.accent_bar, "setBackgroundColor", ev.accentColor)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = events.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() {}
}
