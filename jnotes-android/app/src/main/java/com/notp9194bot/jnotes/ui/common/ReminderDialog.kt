package com.notp9194bot.jnotes.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.notp9194bot.jnotes.data.model.Recurrence
import com.notp9194bot.jnotes.util.DateUtils
import java.util.Calendar

@Composable
fun ReminderDialog(
    initialMillis: Long,
    initialRecurrence: Recurrence = Recurrence.NONE,
    initialInterval: Int = 1,
    onDismiss: () -> Unit,
    onConfirm: (Long, Recurrence, Int) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    var picked by remember { mutableLongStateOf(if (initialMillis > now) initialMillis else now + 60 * 60 * 1000L) }
    var recurrence by remember { mutableStateOf(initialRecurrence) }
    var interval by remember { mutableStateOf(initialInterval.coerceAtLeast(1)) }

    fun openDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = picked }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
                picked = cal.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun openTime() {
        val cal = Calendar.getInstance().apply { timeInMillis = picked }
        TimePickerDialog(
            context,
            { _, h, m ->
                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                picked = cal.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reminds you at: ${DateUtils.formatFull(picked)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = ::openDate) { Text("Pick date") }
                    TextButton(onClick = ::openTime) { Text("Pick time") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { picked = now + 60 * 60 * 1000L }) { Text("In 1h") }
                    TextButton(onClick = { picked = now + 3 * 60 * 60 * 1000L }) { Text("In 3h") }
                    TextButton(onClick = { picked = now + 24 * 60 * 60 * 1000L }) { Text("Tomorrow") }
                }
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Recurrence.values().forEach { r ->
                        AssistChip(
                            onClick = { recurrence = r },
                            label = {
                                Text(
                                    when (r) {
                                        Recurrence.NONE -> "Once"
                                        Recurrence.DAILY -> "Daily"
                                        Recurrence.WEEKLY -> "Weekly"
                                        Recurrence.MONTHLY -> "Monthly"
                                        Recurrence.YEARLY -> "Yearly"
                                    },
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (recurrence == r)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }
                }
                if (recurrence != Recurrence.NONE) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Every")
                        TextButton(onClick = { interval = (interval - 1).coerceAtLeast(1) }) { Text("-") }
                        Text("$interval")
                        TextButton(onClick = { interval = (interval + 1).coerceAtMost(99) }) { Text("+") }
                        Text(
                            when (recurrence) {
                                Recurrence.DAILY -> if (interval == 1) "day" else "days"
                                Recurrence.WEEKLY -> if (interval == 1) "week" else "weeks"
                                Recurrence.MONTHLY -> if (interval == 1) "month" else "months"
                                Recurrence.YEARLY -> if (interval == 1) "year" else "years"
                                Recurrence.NONE -> ""
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (picked > System.currentTimeMillis()) onConfirm(picked, recurrence, interval) else onDismiss()
                },
            ) { Text("Set") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
