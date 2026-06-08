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
        val results = mutableMapOf<String, CountdownEvent>()
 
        // ── Pass 1: Instances API (catches most events + recurring instances) ──
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
            )
 
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleCol = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
 
                while (c.moveToNext()) {
                    val beginMs = c.getLong(beginCol)
                    if (beginMs <= now) continue
                    val eid = c.getString(idCol) ?: continue
                    if (eid in results) continue
                    results[eid] = CountdownEvent(
                        id = eid,
                        title = c.getString(titleCol) ?: "(No title)",
                        startTime = beginMs,
                        accentColor = ACCENT,
                        emoji = "\uD83D\uDCC5"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
 
        // ── Pass 2: Events table fallback for recurring events missed by Instances ──
        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.RRULE
                ),
                "${CalendarContract.Events.RRULE} IS NOT NULL AND ${CalendarContract.Events.RRULE} != ''",
                null, null
            )
 
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleCol = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val dtCol = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val rrCol = c.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
 
                while (c.moveToNext()) {
                    val eid = c.getString(idCol) ?: continue
                    if (eid in results) continue  // already found via Instances
 
                    val dtStart = c.getLong(dtCol)
                    val rrule = c.getString(rrCol) ?: continue
                    val title = c.getString(titleCol) ?: "(No title)"
 
                    val nextOccurrence = computeNextOccurrence(dtStart, rrule, now)
                    if (nextOccurrence != null && nextOccurrence in (now + 1)..end) {
                        results[eid] = CountdownEvent(
                            id = eid,
                            title = title,
                            startTime = nextOccurrence,
                            accentColor = ACCENT,
                            emoji = "\uD83D\uDCC5"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
 
        return results.values.sortedBy { it.startTime }
    }
 
    private fun computeNextOccurrence(dtStart: Long, rrule: String, now: Long): Long? {
        val rule = rrule.uppercase()
 
        // Check if recurrence has ended
        if (rule.contains("UNTIL=")) {
            try {
                val until = rule.substringAfter("UNTIL=").substringBefore(";").substringBefore("\n")
                val sdf = if (until.length == 8)
                    SimpleDateFormat("yyyyMMdd", Locale.US)
                else
                    SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val untilDate = sdf.parse(until)
                if (untilDate != null && untilDate.time < now) return null
            } catch (e: Exception) { /* ignore parse errors, try anyway */ }
        }
 
        val start = Calendar.getInstance().apply { timeInMillis = dtStart }
        val candidate = Calendar.getInstance()
 
        return when {
            rule.contains("FREQ=DAILY") -> {
                candidate.timeInMillis = now
                candidate.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY))
                candidate.set(Calendar.MINUTE, start.get(Calendar.MINUTE))
                candidate.set(Calendar.SECOND, 0)
                if (candidate.timeInMillis <= now) candidate.add(Calendar.DAY_OF_YEAR, 1)
                candidate.timeInMillis
            }
            rule.contains("FREQ=WEEKLY") -> {
                candidate.timeInMillis = now
                candidate.set(Calendar.DAY_OF_WEEK, start.get(Calendar.DAY_OF_WEEK))
                candidate.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY))
                candidate.set(Calendar.MINUTE, start.get(Calendar.MINUTE))
                candidate.set(Calendar.SECOND, 0)
                if (candidate.timeInMillis <= now) candidate.add(Calendar.WEEK_OF_YEAR, 1)
                candidate.timeInMillis
            }
            rule.contains("FREQ=MONTHLY") -> {
                candidate.timeInMillis = now
                val day = start.get(Calendar.DAY_OF_MONTH)
                candidate.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
                candidate.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY))
                candidate.set(Calendar.MINUTE, start.get(Calendar.MINUTE))
                candidate.set(Calendar.SECOND, 0)
                if (candidate.timeInMillis <= now) {
                    candidate.add(Calendar.MONTH, 1)
                    candidate.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                candidate.timeInMillis
            }
            rule.contains("FREQ=YEARLY") -> {
                candidate.timeInMillis = now
                candidate.set(Calendar.MONTH, start.get(Calendar.MONTH))
                val day = start.get(Calendar.DAY_OF_MONTH)
                candidate.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
                candidate.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY))
                candidate.set(Calendar.MINUTE, start.get(Calendar.MINUTE))
                candidate.set(Calendar.SECOND, 0)
                if (candidate.timeInMillis <= now) candidate.add(Calendar.YEAR, 1)
                candidate.timeInMillis
            }
            else -> null
        }
    }
}
 
