package com.notp9194bot.jnotes.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notp9194bot.jnotes.data.model.Folder
import com.notp9194bot.jnotes.data.model.SmartFilter
import com.notp9194bot.jnotes.data.model.ViewMode

@Composable
fun AppDrawerContent(
    activeView: ViewMode,
    activeSmart: SmartFilter?,
    activeTag: String?,
    activeFolderId: String?,
    tags: List<String>,
    folders: List<Folder>,
    onSelectView: (ViewMode) -> Unit,
    onSelectSmart: (SmartFilter) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "jnotes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        NavigationDrawerItem(
            label = { Text("All notes") },
            icon = { Icon(Icons.Outlined.Notes, null) },
            selected = activeView == ViewMode.ALL && activeSmart == null && activeTag == null && activeFolderId == null,
            onClick = { onSelectView(ViewMode.ALL) },
            colors = NavigationDrawerItemDefaults.colors(),
        )
        NavigationDrawerItem(
            label = { Text("Pinned") },
            icon = { Icon(Icons.Outlined.PushPin, null) },
            selected = activeView == ViewMode.PINNED,
            onClick = { onSelectView(ViewMode.PINNED) },
        )
        NavigationDrawerItem(
            label = { Text("Archive") },
            icon = { Icon(Icons.Outlined.Archive, null) },
            selected = activeView == ViewMode.ARCHIVED,
            onClick = { onSelectView(ViewMode.ARCHIVED) },
        )
        NavigationDrawerItem(
            label = { Text("Trash") },
            icon = { Icon(Icons.Outlined.Delete, null) },
            selected = activeView == ViewMode.TRASH,
            onClick = { onSelectView(ViewMode.TRASH) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Smart filters", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))

        NavigationDrawerItem(
            label = { Text("Has reminder") },
            icon = { Icon(Icons.Outlined.NotificationsActive, null) },
            selected = activeSmart == SmartFilter.REMINDER,
            onClick = { onSelectSmart(SmartFilter.REMINDER) },
        )
        NavigationDrawerItem(
            label = { Text("Checklists") },
            icon = { Icon(Icons.Outlined.Checklist, null) },
            selected = activeSmart == SmartFilter.CHECKLIST,
            onClick = { onSelectSmart(SmartFilter.CHECKLIST) },
        )
        NavigationDrawerItem(
            label = { Text("Recent (7d)") },
            icon = { Icon(Icons.Outlined.Schedule, null) },
            selected = activeSmart == SmartFilter.RECENT,
            onClick = { onSelectSmart(SmartFilter.RECENT) },
        )

        if (folders.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Folders", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
            for (f in folders) {
                NavigationDrawerItem(
                    label = { Text(f.name) },
                    icon = { Icon(Icons.Outlined.Folder, null) },
                    selected = activeFolderId == f.id,
                    onClick = { onSelectFolder(if (activeFolderId == f.id) null else f.id) },
                )
            }
        }

        if (tags.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Tags", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
            for (tag in tags) {
                NavigationDrawerItem(
                    label = { Text("#$tag") },
                    icon = { Icon(Icons.Outlined.Tag, null) },
                    selected = activeTag == tag,
                    onClick = { onSelectTag(if (activeTag == tag) null else tag) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Calendar") },
            icon = { Icon(Icons.Outlined.CalendarMonth, null) },
            selected = false,
            onClick = onOpenCalendar,
        )
        NavigationDrawerItem(
            label = { Text("Statistics") },
            icon = { Icon(Icons.Outlined.BarChart, null) },
            selected = false,
            onClick = onOpenStats,
        )
        NavigationDrawerItem(
            label = { Text("Settings") },
            icon = { Icon(Icons.Outlined.Settings, null) },
            selected = false,
            onClick = onOpenSettings,
        )
        NavigationDrawerItem(
            label = { Text("About") },
            icon = { Icon(Icons.Outlined.Info, null) },
            selected = false,
            onClick = onOpenAbout,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
