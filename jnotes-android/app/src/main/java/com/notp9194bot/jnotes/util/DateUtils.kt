package com.notp9194bot.jnotes.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    private val dateYearFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val fullFmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

    fun formatRelative(ts: Long, now: Long = System.currentTimeMillis()): String {
        if (ts <= 0) return ""
        val diff = now - ts
        val sec = diff / 1000
        val min = sec / 60
        val hr = min / 60
        return when {
            sec < 60 -> "just now"
            min < 60 -> "${min}m ago"
            hr < 24 -> "${hr}h ago"
            isSameDay(ts, now - 86_400_000L) -> "yesterday"
            isSameYear(ts, now) -> dateFmt.format(Date(ts))
            else -> dateYearFmt.format(Date(ts))
        }
    }

    fun formatFull(ts: Long): String =
        if (ts <= 0) "" else fullFmt.format(Date(ts))

    fun formatTime(ts: Long): String =
        if (ts <= 0) "" else timeFmt.format(Date(ts))

    fun startOfDay(ts: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameYear(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
    }
}
