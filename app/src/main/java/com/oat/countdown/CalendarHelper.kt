package com.oat.countdown
 
import android.content.ContentUris
import android.content.Context
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
    private val ACCENT = Color.parseColor("#7ff0d6")
 
    fun getEvents(context: Context, rangeDays: Int = 365): List<CountdownEvent> {
        val now = System.currentTimeMillis()
        val end = now + rangeDays.toLong() * 86_400_000
        val events = mutableListOf<CountdownEvent>()
        val seen = mutableSetOf<String>()
 
        try {
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(uri, now)
            ContentUris.appendId(uri, end)
 
            val cursor = context.contentResolver.query(
                uri.build(),
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN
                ),
                null, null,
                "${CalendarContract.Instances.BEGIN} ASC"
            ) ?: return events
 
            cursor.use { c ->
                val idCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleCol = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
 
                while (c.moveToNext()) {
                    val beginMs = c.getLong(beginCol)
                    if (beginMs <= now) continue          // skip past FIRST
                    val eid = c.getString(idCol) ?: continue
                    if (eid in seen) continue             // THEN dedup
                    seen.add(eid)
                    events.add(CountdownEvent(
                        id = eid,
                        title = c.getString(titleCol) ?: "(No title)",
                        startTime = beginMs,
                        accentColor = ACCENT,
                        emoji = "\uD83D\uDCC5"
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events.sortedBy { it.startTime }
    }
}
 


