package com.oat.countdown

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.*

data class CountdownEvent(
    val id: String,
    val title: String,
    val startTime: Long,
    val accentColor: Int,
    val emoji: String
) {
    val daysLeft: Int
        get() = ((startTime - System.currentTimeMillis()) / 86_400_000).toInt().coerceAtLeast(0)

    val dateString: String
        get() = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(startTime))
}

object CalendarHelper {

    // Google Calendar event color IDs → hex
    private val COLOR_MAP = mapOf(
        1 to Color.parseColor("#7986cb"),
        2 to Color.parseColor("#33b679"),
        3 to Color.parseColor("#8e24aa"),
        4 to Color.parseColor("#e67c73"),
        5 to Color.parseColor("#f6bf26"),
        6 to Color.parseColor("#f4511e"),
        7 to Color.parseColor("#039be5"),
        8 to Color.parseColor("#616161"),
        9 to Color.parseColor("#3f51b5"),
        10 to Color.parseColor("#0b8043"),
        11 to Color.parseColor("#d50000")
    )

    private val DEFAULT_ACCENT = Color.parseColor("#7ff0d6")

    fun getEvents(context: Context, rangeDays: Int = 365): List<CountdownEvent> {
        val events = mutableListOf<CountdownEvent>()
        val now = System.currentTimeMillis()
        val end = now + rangeDays.toLong() * 86_400_000

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.EVENT_COLOR
        )

        val cursor: Cursor? = try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?",
                arrayOf(now.toString(), end.toString()),
                "${CalendarContract.Events.DTSTART} ASC"
            )
        } catch (e: SecurityException) {
            null
        }

        cursor?.use { c ->
            val idIdx = c.getColumnIndex(CalendarContract.Events._ID)
            val titleIdx = c.getColumnIndex(CalendarContract.Events.TITLE)
            val startIdx = c.getColumnIndex(CalendarContract.Events.DTSTART)
            val colorIdx = c.getColumnIndex(CalendarContract.Events.EVENT_COLOR)

            while (c.moveToNext()) {
                val id = c.getString(idIdx) ?: continue
                val title = c.getString(titleIdx) ?: "(No title)"
                val startMs = c.getLong(startIdx)
                val colorVal = if (colorIdx >= 0) c.getInt(colorIdx) else 0
                val accent = if (colorVal != 0) colorVal else DEFAULT_ACCENT

                if (startMs > now) {
                    events.add(
                        CountdownEvent(
                            id = id,
                            title = title,
                            startTime = startMs,
                            accentColor = accent,
                            emoji = "\uD83D\uDCC5"  // 📅
                        )
                    )
                }
            }
        }
        return events
    }
}
