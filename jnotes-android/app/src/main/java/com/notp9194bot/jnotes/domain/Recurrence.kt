package com.notp9194bot.jnotes.domain

import com.notp9194bot.jnotes.data.model.Recurrence
import java.util.Calendar

object RecurrenceCalc {
    fun nextOccurrence(from: Long, rule: Recurrence, interval: Int = 1): Long? {
        if (rule == Recurrence.NONE) return null
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        val n = interval.coerceAtLeast(1)
        when (rule) {
            Recurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, n)
            Recurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, n)
            Recurrence.MONTHLY -> cal.add(Calendar.MONTH, n)
            Recurrence.YEARLY -> cal.add(Calendar.YEAR, n)
            Recurrence.NONE -> return null
        }
        return cal.timeInMillis
    }
}
