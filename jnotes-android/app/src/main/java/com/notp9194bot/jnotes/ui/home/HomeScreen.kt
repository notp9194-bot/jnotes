package com.notp9194bot.jnotes.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.Layout
import com.notp9194bot.jnotes.data.model.SmartFilter
import com.notp9194bot.jnotes.data.model.ViewMode
import com.notp9194bot.jnotes.ui.common.NoteCard
import com.notp9194bot.jnotes.ui.editor.EditorScreen
import com.notp9194bot.jnotes.ui.templates.TemplatesSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(
    onOpenNote: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenCalendar: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsState()
    val settings by ServiceLocator.settings(context).flow.collectAsState(initial = com.notp9194bot.jnotes.data.model.Settings())

    val isDark = isSystemInDarkTheme().let {
        when (settings.theme) {
            com.notp9194bot.jnotes.data.model.ThemeMode.SYSTEM -> it
            com.notp9194bot.jnotes.data.model.ThemeMode.LIGHT -> false
            com.notp9194bot.jnotes.data.model.ThemeMode.DARK -> true
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTemplates by remember { mutableStateOf(false) }
    var selectedPaneNoteId by remember { mutableStateOf<String?>(null) }

    val widthClass = if (activity != null) calculateWindowSizeClass(activity).widthSizeClass else WindowWidthSizeClass.Compact
    val twoPane = widthClass != WindowWidthSizeClass.Compact

    val title = when {
        state.filter.activeTag != null -> "#${state.filter.activeTag}"
        state.filter.activeFolderId != null -> state.folders.firstOrNull { it.id == state.filter.activeFolderId }?.name ?: "Folder"
        state.filter.smartFilter == SmartFilter.REMINDER -> "Has reminder"
        state.filter.smartFilter == SmartFilter.CHECKLIST -> "Checklists"
        state.filter.smartFilter == SmartFilter.RECENT -> "Recent"
        state.filter.view == ViewMode.PINNED -> "Pinned"
        state.filter.view == ViewMode.ARCHIVED -> "Archive"
        state.filter.view == ViewMode.TRASH -> "Trash"
        else -> "All notes"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawerContent(
                    activeView = state.filter.view,
                    activeSmart = state.filter.smartFilter,
                    activeTag = state.filter.activeTag,
                    activeFolderId = state.filter.activeFolderId,
                    tags = state.tags,
                    folders = state.folders,
                    onSelectView = { vm.setView(it); scope.launch { drawerState.close() } },
                    onSelectSmart = { vm.setSmartFilter(it); scope.launch { drawerState.close() } },
                    onSelectTag = { vm.setActiveTag(it); scope.launch { drawerState.close() } },
                    onSelectFolder = { vm.setActiveFolder(it); scope.launch { drawerState.close() } },
                    onOpenStats = { scope.launch { drawerState.close() }; onOpenStats() },
                    onOpenSettings = { scope.launch { drawerState.close() }; onOpenSettings() },
                    onOpenAbout = { scope.launch { drawerState.close() }; onOpenAbout() },
                    onOpenCalendar = { scope.launch { drawerState.close() }; onOpenCalendar() },
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (state.selectedIds.isNotEmpty()) {
                    BulkSelectBar(
                        count = state.selectedIds.size,
                        onClear = vm::clearSelection,
                        onSelectAll = vm::selectAll,
                        onPinToggle = vm::bulkPinToggle,
                        onColor = vm::bulkColor,
                        onArchive = vm::bulkArchive,
                        onTrash = vm::bulkTrash,
                    )
                } else {
                    HomeTopBar(
                        title = title,
                        search = state.filter.search,
                        onSearchChange = vm::setSearch,
                        sort = state.filter.sortBy,
                        onSortChange = vm::setSort,
                        layout = settings.layout,
                        onLayoutToggle = {
                            scope.launch {
                                ServiceLocator.settings(context).setLayout(
                                    if (settings.layout == Layout.GRID) Layout.LIST else Layout.GRID,
                                )
                            }
                        },
                        onMenu = { scope.launch { drawerState.open() } },
                    )
                }
            },
            floatingActionButton = {
                if (state.filter.view == ViewMode.TRASH) {
                    ExtendedFloatingActionButton(
                        onClick = { vm.emptyTrash() },
                        icon = { Icon(Icons.Outlined.DeleteForever, null) },
                        text = { Text("Empty trash") },
                    )
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showTemplates = true },
                        icon = { Icon(Icons.Outlined.Add, null) },
                        text = { Text("New note") },
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (twoPane) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            ContentList(
                                state = state,
                                settings = settings,
                                isDark = isDark,
                                onClick = { n ->
                                    if (state.selectedIds.isNotEmpty()) vm.toggleSelect(n.id)
                                    else { selectedPaneNoteId = n.id }
                                },
                                onLongClick = { n -> vm.toggleSelect(n.id) },
                                onRestore = { vm.restoreSingle(it) },
                                onDeleteForever = { id ->
                                    vm.deletePermanently(id)
                                    scope.launch { snackbarHostState.showSnackbar("Deleted permanently") }
                                },
                            )
                        }
                        androidx.compose.material3.VerticalDivider()
                        Box(modifier = Modifier.weight(1.4f).fillMaxHeight()) {
                            val openId = selectedPaneNoteId ?: state.notes.firstOrNull()?.id
                            if (openId != null) {
                                EditorScreen(
                                    noteId = openId,
                                    onBack = { selectedPaneNoteId = null },
                                    onNavigateToNote = { selectedPaneNoteId = it },
                                )
                            } else {
                                com.notp9194bot.jnotes.ui.common.EmptyState(
                                    title = "Select a note",
                                    subtitle = "Pick a note on the left to view it here.",
                                    icon = Icons.Outlined.Notes,
                                )
                            }
                        }
                    }
                } else {
                    if (state.notes.isEmpty()) {
                        com.notp9194bot.jnotes.ui.common.EmptyState(
                            title = "No notes yet",
                            subtitle = "Tap the + button to create your first note.",
                            icon = Icons.Outlined.Notes,
                        )
                    } else {
                        ContentList(
                            state = state,
                            settings = settings,
                            isDark = isDark,
                            onClick = { n ->
                                if (state.selectedIds.isNotEmpty()) vm.toggleSelect(n.id) else onOpenNote(n.id)
                            },
                            onLongClick = { n -> vm.toggleSelect(n.id) },
                            onRestore = { vm.restoreSingle(it) },
                            onDeleteForever = { id ->
                                vm.deletePermanently(id)
                                scope.launch { snackbarHostState.showSnackbar("Deleted permanently") }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showTemplates) {
        TemplatesSheet(
            onDismiss = { showTemplates = false },
            onPickBlank = {
                showTemplates = false
                scope.launch { onOpenNote(vm.createBlank()) }
            },
            onPickTemplate = { t ->
                showTemplates = false
                scope.launch { onOpenNote(vm.createFromTemplate(t)) }
            },
        )
    }
}

@Composable
private fun ContentList(
    state: HomeUiState,
    settings: com.notp9194bot.jnotes.data.model.Settings,
    isDark: Boolean,
    onClick: (com.notp9194bot.jnotes.data.model.Note) -> Unit,
    onLongClick: (com.notp9194bot.jnotes.data.model.Note) -> Unit,
    onRestore: (com.notp9194bot.jnotes.data.model.Note) -> Unit,
    onDeleteForever: (String) -> Unit,
) {
    val inTrash = state.filter.view == ViewMode.TRASH
    val pinned = if (!inTrash) state.notes.filter { it.pinned } else emptyList()
    val others = if (!inTrash) state.notes.filter { !it.pinned } else state.notes

    if (settings.layout == Layout.GRID) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 170.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            if (pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    SectionHeader("Pinned")
                }
                gridItems(items = pinned, key = { "p-" + it.id }) { n ->
                    NoteCard(note = n, selected = n.id in state.selectedIds, isDark = isDark, onClick = { onClick(n) }, onLongClick = { onLongClick(n) })
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    SectionHeader("Others")
                }
            }
            gridItems(items = others, key = { it.id }) { n ->
                NoteCard(note = n, selected = n.id in state.selectedIds, isDark = isDark, onClick = { onClick(n) }, onLongClick = { onLongClick(n) })
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            if (pinned.isNotEmpty()) {
                item { SectionHeader("Pinned") }
                items(items = pinned, key = { "p-" + it.id }) { n ->
                    NoteCard(note = n, selected = n.id in state.selectedIds, isDark = isDark, onClick = { onClick(n) }, onLongClick = { onLongClick(n) })
                }
                item { SectionHeader("Others") }
            }
            items(items = others, key = { it.id }) { n ->
                if (inTrash) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            NoteCard(note = n, selected = n.id in state.selectedIds, isDark = isDark, onClick = { onClick(n) }, onLongClick = { onLongClick(n) })
                        }
                        androidx.compose.material3.IconButton(onClick = { onRestore(n) }) {
                            Icon(Icons.Outlined.Restore, contentDescription = "Restore")
                        }
                        androidx.compose.material3.IconButton(onClick = { onDeleteForever(n.id) }) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete forever")
                        }
                    }
                } else {
                    NoteCard(note = n, selected = n.id in state.selectedIds, isDark = isDark, onClick = { onClick(n) }, onLongClick = { onLongClick(n) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}
