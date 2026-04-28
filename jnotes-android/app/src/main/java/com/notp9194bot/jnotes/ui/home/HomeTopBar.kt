package com.notp9194bot.jnotes.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notp9194bot.jnotes.data.model.Layout
import com.notp9194bot.jnotes.data.model.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    title: String,
    search: String,
    onSearchChange: (String) -> Unit,
    sort: SortBy,
    onSortChange: (SortBy) -> Unit,
    layout: Layout,
    onLayoutToggle: () -> Unit,
    onMenu: () -> Unit,
) {
    var searchOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (searchOpen) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search notes…") },
                    singleLine = true,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            } else {
                Text(title)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = {
                searchOpen = !searchOpen
                if (!searchOpen) onSearchChange("")
            }) {
                Icon(
                    if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = "Search",
                )
            }
            IconButton(onClick = onLayoutToggle) {
                Icon(
                    if (layout == Layout.GRID) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                    contentDescription = "Toggle layout",
                )
            }
            IconButton(onClick = { sortOpen = true }) {
                Icon(Icons.Outlined.Sort, contentDescription = "Sort")
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                DropdownMenuItem(text = { Text("Last updated") }, onClick = { onSortChange(SortBy.UPDATED); sortOpen = false })
                DropdownMenuItem(text = { Text("Created") }, onClick = { onSortChange(SortBy.CREATED); sortOpen = false })
                DropdownMenuItem(text = { Text("Title A→Z") }, onClick = { onSortChange(SortBy.TITLE_AZ); sortOpen = false })
                DropdownMenuItem(text = { Text("Title Z→A") }, onClick = { onSortChange(SortBy.TITLE_ZA); sortOpen = false })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
