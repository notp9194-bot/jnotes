package com.notp9194bot.jnotes.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notp9194bot.jnotes.data.model.Layout
import com.notp9194bot.jnotes.data.model.SortBy
import com.notp9194bot.jnotes.data.model.ThemeMode
import com.notp9194bot.jnotes.ui.lock.BiometricGate
import com.notp9194bot.jnotes.ui.theme.Accents
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val s by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pinDialog by remember { mutableStateOf(false) }
    var customAccentDialog by remember { mutableStateOf(false) }
    var webdavDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = vm.exportNow(uri)
            snackbar.showSnackbar(if (ok) "Exported successfully" else "Export failed")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val n = vm.importNow(uri)
            snackbar.showSnackbar("Imported $n notes")
        }
    }

    val mdImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = vm.importMarkdown(uri)
            snackbar.showSnackbar(if (ok) "Imported markdown note" else "Import failed")
        }
    }

    val mdExportAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) scope.launch {
            val n = vm.exportAllMarkdown(uri)
            snackbar.showSnackbar("Exported $n markdown files")
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            vm.setAutoBackup(true, uri.toString())
            scope.launch { snackbar.showSnackbar("Auto-backup enabled") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = s.theme == mode,
                            onClick = { vm.setTheme(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }

                HorizontalDivider()
                Text("Accent", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Accents.forEachIndexed { idx, a ->
                        FilterChip(
                            selected = s.accentIdx == idx && s.customAccentArgb == null,
                            onClick = { vm.setAccent(idx) },
                            label = { Text(a.name) },
                        )
                    }
                    FilterChip(
                        selected = s.customAccentArgb != null,
                        onClick = { customAccentDialog = true },
                        label = { Text("Custom…") },
                    )
                }

                HorizontalDivider()
                ToggleRow(
                    title = "Dynamic color",
                    subtitle = "Use Material You wallpaper colors (Android 12+)",
                    checked = s.dynamicColor,
                    onChange = vm::setDynamicColor,
                )
                ToggleRow(
                    title = "AMOLED black",
                    subtitle = "Pure black backgrounds in dark mode (saves battery on OLED).",
                    checked = s.amoled,
                    onChange = vm::setAmoled,
                )
                HorizontalDivider()
                Text("Font scale: ${"%.2f".format(s.fontScale)}x", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = s.fontScale,
                    onValueChange = { vm.setFontScale((it * 100).toInt() / 100f) },
                    valueRange = 0.7f..1.6f,
                    steps = 17,
                )
            }

            SectionCard(title = "Layout & sorting") {
                Text("Layout", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = s.layout == Layout.GRID, onClick = { vm.setLayout(Layout.GRID) }, label = { Text("Grid") })
                    FilterChip(selected = s.layout == Layout.LIST, onClick = { vm.setLayout(Layout.LIST) }, label = { Text("List") })
                }
                HorizontalDivider()
                Text("Sort by", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = s.sortBy == SortBy.UPDATED, onClick = { vm.setSort(SortBy.UPDATED) }, label = { Text("Updated") })
                        FilterChip(selected = s.sortBy == SortBy.CREATED, onClick = { vm.setSort(SortBy.CREATED) }, label = { Text("Created") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = s.sortBy == SortBy.TITLE_AZ, onClick = { vm.setSort(SortBy.TITLE_AZ) }, label = { Text("Title A→Z") })
                        FilterChip(selected = s.sortBy == SortBy.TITLE_ZA, onClick = { vm.setSort(SortBy.TITLE_ZA) }, label = { Text("Title Z→A") })
                    }
                }
            }

            SectionCard(title = "Trash auto-purge") {
                Text(
                    "Items in trash older than ${s.purgeDays} day${if (s.purgeDays == 1) "" else "s"} are deleted automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = s.purgeDays.toFloat(),
                    onValueChange = { vm.setPurgeDays(it.toInt().coerceIn(1, 90)) },
                    valueRange = 1f..90f,
                    steps = 88,
                )
            }

            SectionCard(title = "Backup & sync") {
                Text("JSON export/import (full backup)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("jnotes-backup.json") }) { Text("Export JSON") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("Import JSON") }
                }
                HorizontalDivider()
                Text("Markdown", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { mdExportAllLauncher.launch(null) }) { Text("Export all .md") }
                    OutlinedButton(onClick = { mdImportLauncher.launch(arrayOf("text/markdown", "text/*", "*/*")) }) { Text("Import .md") }
                }
                HorizontalDivider()
                ToggleRow(
                    title = "Daily auto-backup",
                    subtitle = if (s.autoBackupEnabled && s.autoBackupFolderUri != null) "Saves a JSON backup once a day. Keeps the 14 newest." else "Pick a folder to enable.",
                    checked = s.autoBackupEnabled,
                    onChange = { enabled ->
                        if (enabled && s.autoBackupFolderUri.isNullOrEmpty()) {
                            backupFolderLauncher.launch(null)
                        } else {
                            vm.setAutoBackup(enabled, s.autoBackupFolderUri)
                        }
                    },
                )
                if (s.autoBackupEnabled) {
                    OutlinedButton(onClick = { backupFolderLauncher.launch(null) }) { Text("Change backup folder") }
                }
                HorizontalDivider()
                Text("WebDAV sync", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (s.webdavUrl.isNullOrBlank()) "Not configured."
                    else "Active. Last sync: ${if (s.lastSyncAt > 0) com.notp9194bot.jnotes.util.DateUtils.formatFull(s.lastSyncAt) else "never"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { webdavDialog = true }) { Text(if (s.webdavUrl.isNullOrBlank()) "Configure" else "Edit") }
                    if (!s.webdavUrl.isNullOrBlank()) {
                        OutlinedButton(onClick = { vm.setWebDav(null, null, null) }) { Text("Disable") }
                    }
                }
            }

            SectionCard(title = "Privacy & security") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PIN lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (s.pinHash != null) "PIN is set. App requires it on launch." else "Not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    if (s.pinHash != null) {
                        TextButton(onClick = { vm.setPin(null) }) { Text("Remove") }
                    }
                    Button(onClick = { pinDialog = true }) { Text(if (s.pinHash != null) "Change" else "Set PIN") }
                }

                ToggleRow(
                    title = "Fingerprint & face unlock",
                    subtitle = "Use the strongest biometric available (fingerprint, secure face, or device PIN) instead of the app PIN.",
                    checked = s.biometricEnabled,
                    onChange = vm::setBiometric,
                )

                // Live biometric status + tester
                run {
                    val ctx = LocalContext.current
                    val activity = ctx as? FragmentActivity
                    val availability = remember(s.biometricEnabled) { BiometricGate.check(ctx) }
                    val statusText = when (availability) {
                        BiometricGate.Availability.AVAILABLE -> "✓ Ready — fingerprint or face unlock detected on this device."
                        BiometricGate.Availability.NONE_ENROLLED -> "No fingerprint/face is set up. Add one in your device Settings → Security."
                        BiometricGate.Availability.NO_HARDWARE -> "This device doesn't have biometric hardware."
                        BiometricGate.Availability.HW_UNAVAILABLE -> "Biometric hardware is busy. Try again in a moment."
                        BiometricGate.Availability.SECURITY_UPDATE_REQUIRED -> "A device security update is required to use biometric unlock."
                        BiometricGate.Availability.UNSUPPORTED -> "Biometric unlock is unsupported on this Android version."
                        BiometricGate.Availability.UNKNOWN -> "Biometric status is unknown right now."
                    }
                    val statusColor =
                        if (availability == BiometricGate.Availability.AVAILABLE) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = activity != null && availability == BiometricGate.Availability.AVAILABLE,
                            onClick = {
                                if (activity != null) {
                                    BiometricGate.prompt(
                                        activity = activity,
                                        title = "Test biometric unlock",
                                        subtitle = "Authenticate to confirm it works",
                                        onSuccess = {
                                            scope.launch { snackbar.showSnackbar("Biometric unlock works ✓") }
                                        },
                                        onError = { msg ->
                                            scope.launch { snackbar.showSnackbar("Biometric: $msg") }
                                        },
                                        onFailed = {
                                            scope.launch { snackbar.showSnackbar("Not recognised — try again") }
                                        },
                                    )
                                }
                            },
                        ) { Text("Test fingerprint / face") }
                    }
                }

                HorizontalDivider()

                Text("Auto-lock", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        0 to "Never",
                        15 to "15 s",
                        60 to "1 min",
                        300 to "5 min",
                        900 to "15 min",
                    ).forEach { (seconds, label) ->
                        FilterChip(
                            selected = s.autoLockSeconds == seconds,
                            onClick = { vm.setAutoLockSeconds(seconds) },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    "Re-lock after this much time in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                ToggleRow(
                    title = "Lock immediately on exit",
                    subtitle = "The app re-locks the moment you switch away — no timer.",
                    checked = s.lockOnExit,
                    onChange = vm::setLockOnExit,
                )

                HorizontalDivider()

                ToggleRow(
                    title = "Block screenshots & screen recording",
                    subtitle = "Apps and recorders see a black screen instead of your notes.",
                    checked = s.screenshotBlock,
                    onChange = vm::setScreenshotBlock,
                )
                ToggleRow(
                    title = "Hide content in recent apps",
                    subtitle = "Note text is not shown in the multitasking thumbnail.",
                    checked = s.hideInRecents,
                    onChange = vm::setHideInRecents,
                )
            }

            SectionCard(title = "Persistent note") {
                ToggleRow(
                    title = "Pin a note in the notification shade",
                    subtitle = "Keeps your most-pinned note one swipe away. Tap to open it.",
                    checked = s.persistentNoteEnabled,
                    onChange = { vm.setPersistent(it) },
                )
            }

            SectionCard(title = "Language") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("system" to "System", "en" to "English", "hi" to "हिन्दी").forEach { (code, label) ->
                        FilterChip(selected = s.language == code, onClick = { vm.setLanguage(code) }, label = { Text(label) })
                    }
                }
                Text(
                    "Language preference is stored. Full app translations can be added via Android resources later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            SectionCard(title = "About") {
                Text("jnotes — native Android edition v2", style = MaterialTheme.typography.titleMedium)
                Text("Fully offline notes app.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (pinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pinDialog = false },
            title = { Text("Set PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(8) },
                    placeholder = { Text("4–8 digits") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length in 4..8) {
                            vm.setPin(newPin)
                            pinDialog = false
                        }
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { pinDialog = false }) { Text("Cancel") } },
        )
    }

    if (customAccentDialog) {
        val swatches = listOf(
            0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5,
            0xFF039BE5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33,
            0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E, 0xFF6D4C41, 0xFF546E7A,
        ).map { it.toInt() }
        AlertDialog(
            onDismissRequest = { customAccentDialog = false },
            title = { Text("Custom accent color") },
            text = {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    swatches.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(argb), CircleShape)
                                .clickable {
                                    vm.setCustomAccent(argb)
                                    customAccentDialog = false
                                },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setCustomAccent(null)
                    customAccentDialog = false
                }) { Text("Reset to preset") }
            },
            dismissButton = { TextButton(onClick = { customAccentDialog = false }) { Text("Cancel") } },
        )
    }

    if (webdavDialog) {
        var url by remember { mutableStateOf(s.webdavUrl.orEmpty()) }
        var user by remember { mutableStateOf(s.webdavUser.orEmpty()) }
        var pass by remember { mutableStateOf(s.webdavPass.orEmpty()) }
        AlertDialog(
            onDismissRequest = { webdavDialog = false },
            title = { Text("WebDAV settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        placeholder = { Text("https://server/path") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = user, onValueChange = { user = it },
                        placeholder = { Text("Username") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = pass, onValueChange = { pass = it },
                        placeholder = { Text("Password") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Text(
                        "Backup uploads to <url>/jnotes-backup.json every 6 hours when on Wi-Fi/data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setWebDav(url.trim().ifBlank { null }, user.trim().ifBlank { null }, pass.ifBlank { null })
                    webdavDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { webdavDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
