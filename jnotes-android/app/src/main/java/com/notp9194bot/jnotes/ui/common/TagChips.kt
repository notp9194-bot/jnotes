package com.notp9194bot.jnotes.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipsEditor(
    tags: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTag by remember { mutableStateOf("") }
    FlowRow(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (tag in tags) {
            AssistChip(
                onClick = { onChange(tags.filterNot { it == tag }) },
                label = { Text("#$tag") },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove $tag",
                    )
                },
                colors = AssistChipDefaults.assistChipColors(),
                border = AssistChipDefaults.assistChipBorder(enabled = true),
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it.replace(' ', '-') },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Add tag") },
            singleLine = true,
        )
        TextButton(
            onClick = {
                val t = newTag.trim().removePrefix("#")
                if (t.isNotEmpty() && t !in tags) {
                    onChange(tags + t)
                    newTag = ""
                }
            },
            enabled = newTag.isNotBlank(),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("  Add")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChipsRemovable(
    tags: List<String>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (tag in tags) {
            AssistChip(
                onClick = { onRemove(tag) },
                label = { Text("#$tag") },
                trailingIcon = {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove $tag")
                },
            )
        }
    }
}
