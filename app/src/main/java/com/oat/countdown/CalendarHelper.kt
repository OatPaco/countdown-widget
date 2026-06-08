package com.oat.countdown
 
import android.content.ContentUris
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
 
    private val DEFAULT_ACCENT = Color.parseColor("#7ff0d6")
 
    fun getEvents(context: Context, rangeDays: Int = 365): List<CountdownEvent> {
        val events = mutableListOf<CountdownEvent>()
        val now = System.currentTimeMillis()
        val end = now + rangeDays.toLong() * 86_400_000
 
        // Use Instances API — returns BOTH one-time events AND
        // next occurrences of recurring events in the date range.
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)
 
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.EVENT_COLOR
        )
 
        val cursor: Cursor? = try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )
        } catch (e: SecurityException) {
            null
        }
 
        // Keep only the nearest future instance per event (dedup recurring)
        val seen = mutableSetOf<String>()
 
        cursor?.use { c ->
            val eventIdIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIdx = c.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = c.getColumnIndex(CalendarContract.Instances.BEGIN)
            val colorIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
 
            while (c.moveToNext()) {
                val eventId = c.getString(eventIdIdx) ?: continue
                if (eventId in seen) continue  // already got the nearest instance
                seen.add(eventId)
 
                val title = c.getString(titleIdx) ?: "(No title)"
                val beginMs = c.getLong(beginIdx)
                val colorVal = if (colorIdx >= 0) c.getInt(colorIdx) else 0
                val accent = if (colorVal != 0) colorVal else DEFAULT_ACCENT
 
                if (beginMs > now) {
                    events.add(
                        CountdownEvent(
                            id = eventId,
                            title = title,
                            startTime = beginMs,
                            accentColor = accent,
                            emoji = "\uD83D\uDCC5"
                        )
                    )
                }
            }
        }
        return events.sortedBy { it.startTime }
    }
}
 


