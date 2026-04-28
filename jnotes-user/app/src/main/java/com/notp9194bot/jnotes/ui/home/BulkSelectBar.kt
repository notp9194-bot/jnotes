package com.notp9194bot.jnotes.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notp9194bot.jnotes.ui.theme.NoteColors

@Composable
fun BulkSelectBar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onPinToggle: () -> Unit,
    onColor: (Int) -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
) {
    var paletteOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
        }
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        IconButton(onClick = onSelectAll) { Icon(Icons.Outlined.DoneAll, contentDescription = "Select all") }
        IconButton(onClick = onPinToggle) { Icon(Icons.Outlined.PushPin, contentDescription = "Pin") }
        IconButton(onClick = { paletteOpen = true }) {
            Icon(Icons.Outlined.Palette, contentDescription = "Color")
        }
        DropdownMenu(expanded = paletteOpen, onDismissRequest = { paletteOpen = false }) {
            NoteColors.forEachIndexed { idx, palette ->
                DropdownMenuItem(
                    text = { Text(palette.name) },
                    onClick = { onColor(idx); paletteOpen = false },
                )
            }
        }
        IconButton(onClick = onArchive) { Icon(Icons.Outlined.Archive, contentDescription = "Archive") }
        IconButton(onClick = onTrash) { Icon(Icons.Outlined.Delete, contentDescription = "Trash") }
    }
}
