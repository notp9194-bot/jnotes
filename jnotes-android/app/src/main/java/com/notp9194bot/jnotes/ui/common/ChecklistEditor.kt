package com.notp9194bot.jnotes.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.notp9194bot.jnotes.data.model.ChecklistItem
import com.notp9194bot.jnotes.data.model.newNoteId

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onChange: (List<ChecklistItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((index, item) in items.withIndex()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { checked ->
                        onChange(items.toMutableList().also { it[index] = item.copy(checked = checked) })
                    },
                )
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { v ->
                        onChange(items.toMutableList().also { it[index] = item.copy(text = v) })
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("List item") },
                    textStyle = if (item.checked) {
                        MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough)
                    } else MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                IconButton(
                    onClick = {
                        onChange(items.toMutableList().also { it.removeAt(index) })
                    },
                ) { Icon(Icons.Outlined.Close, contentDescription = "Remove") }
            }
        }
        TextButton(
            onClick = {
                onChange(items + ChecklistItem(id = newNoteId(), text = "", checked = false))
            },
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Add item")
        }
    }
}
