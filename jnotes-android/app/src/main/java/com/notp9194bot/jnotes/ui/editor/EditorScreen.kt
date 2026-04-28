package com.notp9194bot.jnotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import com.notp9194bot.jnotes.data.model.NoteType
import kotlinx.coroutines.launch
import com.notp9194bot.jnotes.ui.common.ChecklistEditor
import com.notp9194bot.jnotes.ui.common.ColorPickerRow
import com.notp9194bot.jnotes.ui.common.MarkdownView
import com.notp9194bot.jnotes.ui.common.ReminderDialog
import com.notp9194bot.jnotes.ui.common.TagChipsEditor
import com.notp9194bot.jnotes.ui.theme.NoteColors
import com.notp9194bot.jnotes.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String,
    onBack: () -> Unit,
    onNavigateToNote: (String) -> Unit,
) {
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
    val state by vm.state.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val note = state.note
    var showReminder by remember { mutableStateOf(false) }
    var paletteOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }

    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var currentMatch by remember { mutableStateOf(0) }

    var showDeleteContent by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    val pickImage = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            vm.addAttachment(uri.toString(), "image")
            if (note?.type == NoteType.TEXT) vm.insertAtCursor("\n![image](${uri})\n")
        }
    }

    val pickVideo = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            vm.addAttachment(uri.toString(), "video")
        }
    }

    val pickAudio = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            vm.addAttachment(uri.toString(), "audio")
        }
    }

    // Pre-allocated FileProvider URIs for camera capture
    var pendingCameraPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingCameraVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showAudioRecorder by remember { mutableStateOf(false) }
    var fullscreenImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val takePhoto = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraPhotoUri?.let { vm.addAttachment(it.toString(), "image") }
        }
        pendingCameraPhotoUri = null
    }

    val captureVideo = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CaptureVideo(),
    ) { success ->
        if (success) {
            pendingCameraVideoUri?.let { vm.addAttachment(it.toString(), "video") }
        }
        pendingCameraVideoUri = null
    }

    fun newMediaUri(ext: String): android.net.Uri {
        val dir = java.io.File(context.filesDir, "attachments").apply { mkdirs() }
        val file = java.io.File(dir, "media_${System.currentTimeMillis()}.$ext")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    val exportPdf = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: android.net.Uri? ->
        if (uri != null && note != null) {
            com.notp9194bot.jnotes.util.PdfExport.export(context, note, uri)
        }
    }

    val exportMd = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri: android.net.Uri? ->
        if (uri != null && note != null) {
            com.notp9194bot.jnotes.util.MdImportExport.exportSingle(context, note, uri)
        }
    }

    // autosave on stop / dispose
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.saveNow()
        }
    }

    if (note == null) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Loading…") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding))
        }
        return
    }

    val palette = NoteColors[note.colorIdx.coerceIn(0, NoteColors.lastIndex)]
    val bg = if (isDark) palette.dark else palette.light

    // body editor field state (with selection control for search)
    var bodyTfv by remember(note.id) {
        mutableStateOf(TextFieldValue(note.body, TextRange(note.body.length)))
    }
    LaunchedEffect(note.body) {
        if (bodyTfv.text != note.body) {
            val newLen = note.body.length
            val sel = bodyTfv.selection
            val newSel = TextRange(sel.start.coerceAtMost(newLen), sel.end.coerceAtMost(newLen))
            bodyTfv = TextFieldValue(note.body, newSel)
        }
    }

    val matches = remember(note.body, searchQuery, caseSensitive) {
        findAllMatches(note.body, searchQuery, caseSensitive)
    }
    LaunchedEffect(matches) {
        if (currentMatch >= matches.size) currentMatch = 0
    }

    fun gotoMatch(idx: Int) {
        if (matches.isEmpty()) return
        val safe = ((idx % matches.size) + matches.size) % matches.size
        currentMatch = safe
        val r = matches[safe]
        bodyTfv = bodyTfv.copy(selection = TextRange(r.first, r.last + 1))
    }

    fun replaceCurrent() {
        if (matches.isEmpty() || searchQuery.isEmpty()) return
        val r = matches[currentMatch.coerceAtMost(matches.lastIndex)]
        val newBody = note.body.substring(0, r.first) + replaceText + note.body.substring(r.last + 1)
        vm.update { it.copy(body = newBody) }
    }

    fun replaceAll() {
        if (matches.isEmpty() || searchQuery.isEmpty()) return
        val sb = StringBuilder()
        var lastEnd = 0
        for (r in matches) {
            sb.append(note.body, lastEnd, r.first)
            sb.append(replaceText)
            lastEnd = r.last + 1
        }
        sb.append(note.body, lastEnd, note.body.length)
        vm.update { it.copy(body = sb.toString()) }
        currentMatch = 0
    }

    fun shareNote() {
        val text = buildString {
            if (note.title.isNotBlank()) appendLine(note.title).appendLine()
            if (note.type == NoteType.CHECKLIST) {
                note.items.forEach { item ->
                    appendLine((if (item.checked) "[x] " else "[ ] ") + item.text)
                }
            } else {
                append(note.body)
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share note"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        DateUtils.formatRelative(note.updatedAt).ifBlank { "New note" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.saveNow(); onBack() }) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                },
                actions = {
                    // Slim top bar — only the most-used controls live here so
                    // the back arrow never collides with the search/replace icon.
                    if (note.type == NoteType.TEXT) {
                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(
                                Icons.Outlined.FindReplace,
                                tint = if (searchOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                contentDescription = "Find & replace",
                            )
                        }
                    }
                    IconButton(onClick = { vm.pinToggle() }) {
                        Icon(
                            Icons.Outlined.PushPin,
                            tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            contentDescription = if (note.pinned) "Unpin" else "Pin",
                        )
                    }
                    IconButton(onClick = { moreOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                    }
                    // Color palette is opened from inside the more menu so it
                    // gets its own positioned popup.
                    DropdownMenu(expanded = paletteOpen, onDismissRequest = { paletteOpen = false }) {
                        NoteColors.forEachIndexed { idx, p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { vm.setColor(idx); paletteOpen = false })
                        }
                    }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (state.readingMode) "Editing mode" else "Reading mode") },
                            onClick = { moreOpen = false; vm.toggleReading() },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (note.type == NoteType.CHECKLIST) "Switch to text note" else "Switch to checklist")
                            },
                            onClick = {
                                moreOpen = false
                                vm.setType(if (note.type == NoteType.CHECKLIST) NoteType.TEXT else NoteType.CHECKLIST)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Reminder…") },
                            onClick = { moreOpen = false; showReminder = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Color…") },
                            onClick = { moreOpen = false; paletteOpen = true },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { moreOpen = false; shareNote() },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy all") },
                            onClick = {
                                moreOpen = false
                                val text = if (note.type == NoteType.CHECKLIST)
                                    note.items.joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
                                else note.body
                                clipboard.setText(AnnotatedString(text))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Paste at end") },
                            onClick = {
                                moreOpen = false
                                val pasted = clipboard.getText()?.text.orEmpty()
                                if (pasted.isNotEmpty() && note.type == NoteType.TEXT) {
                                    vm.update { it.copy(body = if (it.body.isEmpty()) pasted else it.body + "\n" + pasted) }
                                }
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Word & character count") },
                            onClick = { moreOpen = false; showInfoDialog = true },
                        )
                        HorizontalDivider()
                        if (note.type == NoteType.TEXT) {
                            DropdownMenuItem(
                                text = { Text("UPPERCASE") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = it.body.uppercase()) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("lowercase") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = it.body.lowercase()) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Title Case") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = toTitleCase(it.body)) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Sort lines A-Z") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = it.body.lines().sorted().joinToString("\n")) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Sort lines Z-A") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = it.body.lines().sortedDescending().joinToString("\n")) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Reverse lines") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = it.body.lines().reversed().joinToString("\n")) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Remove duplicate lines") },
                                onClick = {
                                    moreOpen = false
                                    vm.update {
                                        val seen = LinkedHashSet<String>()
                                        it.body.lines().forEach { l -> seen.add(l) }
                                        it.copy(body = seen.joinToString("\n"))
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Trim line spaces") },
                                onClick = {
                                    moreOpen = false
                                    vm.update { it.copy(body = it.body.lines().joinToString("\n") { l -> l.trim() }) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Collapse blank lines") },
                                onClick = {
                                    moreOpen = false
                                    vm.update {
                                        val out = it.body.replace(Regex("\\n{3,}"), "\n\n")
                                        it.copy(body = out)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Add line numbers") },
                                onClick = {
                                    moreOpen = false
                                    vm.update {
                                        val numbered = it.body.lines().mapIndexed { i, l -> "${i + 1}. $l" }.joinToString("\n")
                                        it.copy(body = numbered)
                                    }
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear body…") },
                                onClick = { moreOpen = false; showDeleteContent = true },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Add image…") },
                            onClick = { moreOpen = false; pickImage.launch("image/*") },
                        )
                        DropdownMenuItem(
                            text = { Text("Add video…") },
                            onClick = { moreOpen = false; pickVideo.launch("video/*") },
                        )
                        DropdownMenuItem(
                            text = { Text("Add audio…") },
                            onClick = { moreOpen = false; pickAudio.launch("audio/*") },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Take photo") },
                            onClick = {
                                moreOpen = false
                                val uri = newMediaUri("jpg")
                                pendingCameraPhotoUri = uri
                                runCatching { takePhoto.launch(uri) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Record video") },
                            onClick = {
                                moreOpen = false
                                val uri = newMediaUri("mp4")
                                pendingCameraVideoUri = uri
                                runCatching { captureVideo.launch(uri) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Record audio") },
                            onClick = { moreOpen = false; showAudioRecorder = true },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Move to folder…") },
                            onClick = { moreOpen = false; showFolderPicker = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Export as PDF") },
                            onClick = {
                                moreOpen = false
                                val safe = (note.title.ifBlank { "note" })
                                    .replace(Regex("[^A-Za-z0-9 _-]"), "_").take(60)
                                exportPdf.launch("$safe.pdf")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export as Markdown") },
                            onClick = {
                                moreOpen = false
                                val safe = (note.title.ifBlank { "note" })
                                    .replace(Regex("[^A-Za-z0-9 _-]"), "_").take(60)
                                exportMd.launch("$safe.md")
                            },
                        )
                    }
                    if (note.trashed) {
                        IconButton(onClick = { vm.restore(); onBack() }) {
                            Icon(Icons.Outlined.Restore, contentDescription = "Restore")
                        }
                    } else {
                        IconButton(onClick = { vm.archive(); onBack() }) {
                            Icon(Icons.Outlined.Archive, contentDescription = "Archive")
                        }
                        IconButton(onClick = { vm.trash(); onBack() }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Trash")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = bg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bg)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (searchOpen && note.type == NoteType.TEXT) {
                SearchReplaceBar(
                    query = searchQuery,
                    replace = replaceText,
                    caseSensitive = caseSensitive,
                    matchCount = matches.size,
                    currentMatch = if (matches.isEmpty()) 0 else currentMatch + 1,
                    onQueryChange = { searchQuery = it; currentMatch = 0 },
                    onReplaceChange = { replaceText = it },
                    onToggleCase = { caseSensitive = !caseSensitive },
                    onPrev = { gotoMatch(currentMatch - 1) },
                    onNext = { gotoMatch(currentMatch + 1) },
                    onReplaceCurrent = { replaceCurrent() },
                    onReplaceAll = { replaceAll() },
                    onClose = { searchOpen = false; searchQuery = ""; replaceText = "" },
                )
            }

            OutlinedTextField(
                value = note.title,
                onValueChange = { v -> vm.update { it.copy(title = v) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                colors = transparentFieldColors(),
            )

            if (state.readingMode && note.type == NoteType.TEXT) {
                MarkdownView(
                    source = note.body,
                    onLinkedTitleClick = { title ->
                        vm.resolveLinkedTitle(title)?.let { onNavigateToNote(it) }
                    },
                )
            } else if (note.type == NoteType.TEXT) {
                MarkdownToolbar(
                    onInsertAround = { left, right ->
                        val sel = bodyTfv.selection
                        val text = bodyTfv.text
                        val insert = if (sel.collapsed) "$left$right" else left + text.substring(sel.start, sel.end) + right
                        val newText = text.substring(0, sel.start) + insert + text.substring(sel.end)
                        val cursor = sel.start + left.length + (sel.end - sel.start)
                        bodyTfv = TextFieldValue(newText, TextRange(cursor))
                        vm.update { it.copy(body = newText) }
                    },
                    onInsertLine = { prefix ->
                        val sel = bodyTfv.selection
                        val text = bodyTfv.text
                        val lineStart = text.lastIndexOf('\n', (sel.start - 1).coerceAtLeast(0)) + 1
                        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
                        val newCursor = sel.start + prefix.length
                        bodyTfv = TextFieldValue(newText, TextRange(newCursor))
                        vm.update { it.copy(body = newText) }
                    },
                )
                OutlinedTextField(
                    value = bodyTfv,
                    onValueChange = { v ->
                        bodyTfv = v
                        if (v.text != note.body) vm.update { it.copy(body = v.text) }
                    },
                    modifier = Modifier.fillMaxWidth().heightAtLeast(),
                    placeholder = { Text("Start typing… Markdown supported. Link with [[Note Title]]") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = transparentFieldColors(),
                )
                if (note.attachmentUris.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    com.notp9194bot.jnotes.ui.common.NoteAttachmentsView(
                        uris = note.attachmentUris,
                        kinds = note.attachmentKinds,
                        onDelete = { vm.removeAttachment(it) },
                        onImageClick = { idx ->
                            fullscreenImageUri = android.net.Uri.parse(note.attachmentUris[idx])
                        },
                    )
                }
            } else {
                ChecklistEditor(
                    items = note.items,
                    onChange = { items -> vm.update { it.copy(items = items) } },
                )
            }

            ColorPickerRow(
                isDark = isDark,
                selected = note.colorIdx,
                onSelect = { vm.setColor(it) },
            )

            TagChipsEditor(
                tags = note.tags,
                onChange = { vm.update { n -> n.copy(tags = it) } },
            )

            if (state.backlinks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Backlinks", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.backlinks.forEach { back ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { vm.saveNow(); onNavigateToNote(back.id) },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(back.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    back.body.take(120),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }

            if (note.reminderAt > 0L) {
                TextButton(onClick = { showReminder = true }) {
                    Text("Reminder: ${DateUtils.formatFull(note.reminderAt)}")
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    if (showReminder) {
        ReminderDialog(
            initialMillis = note.reminderAt,
            initialRecurrence = note.recurrence,
            initialInterval = note.recurrenceInterval,
            onDismiss = { showReminder = false },
            onConfirm = { at, rec, intvl ->
                vm.setReminder(at, note.title.ifBlank { "Reminder" }, note.body.take(120), rec, intvl)
                showReminder = false
            },
            onClear = {
                vm.clearReminder()
                showReminder = false
            },
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            currentFolderId = note.folderId,
            onDismiss = { showFolderPicker = false },
            onPick = { id -> vm.setFolder(id); showFolderPicker = false },
        )
    }

    if (showDeleteContent) {
        AlertDialog(
            onDismissRequest = { showDeleteContent = false },
            title = { Text("Clear note body?") },
            text = { Text("This will remove all text in the note body. The note itself stays.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.update { it.copy(body = "") }
                    showDeleteContent = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContent = false }) { Text("Cancel") }
            },
        )
    }

    if (showInfoDialog) {
        val body = if (note.type == NoteType.CHECKLIST) note.items.joinToString("\n") { it.text } else note.body
        val charCount = body.length
        val wordCount = body.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        val lineCount = if (body.isEmpty()) 0 else body.lines().size
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Note info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Words: $wordCount")
                    Text("Characters: $charCount")
                    Text("Lines: $lineCount")
                    if (note.type == NoteType.CHECKLIST) {
                        val done = note.items.count { it.checked }
                        Text("Tasks: $done / ${note.items.size} done")
                    }
                    Text("Created: ${DateUtils.formatFull(note.createdAt)}")
                    Text("Updated: ${DateUtils.formatFull(note.updatedAt)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("OK") }
            },
        )
    }

    if (showAudioRecorder) {
        com.notp9194bot.jnotes.ui.common.AudioRecorderDialog(
            onSaved = { uri -> vm.addAttachment(uri.toString(), "audio") },
            onDismiss = { showAudioRecorder = false },
        )
    }

    fullscreenImageUri?.let { uri ->
        com.notp9194bot.jnotes.ui.common.FullscreenPhotoDialog(
            onDismiss = { fullscreenImageUri = null },
            uri = uri,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchReplaceBar(
    query: String,
    replace: String,
    caseSensitive: Boolean,
    matchCount: Int,
    currentMatch: Int,
    onQueryChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onToggleCase: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Find") },
                    singleLine = true,
                )
                IconButton(onClick = onPrev, enabled = matchCount > 0) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Previous")
                }
                IconButton(onClick = onNext, enabled = matchCount > 0) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Next")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replace,
                    onValueChange = onReplaceChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Replace with") },
                    singleLine = true,
                )
                TextButton(onClick = onReplaceCurrent, enabled = matchCount > 0) {
                    Text("Replace")
                }
                TextButton(onClick = onReplaceAll, enabled = matchCount > 0) {
                    Text("All")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onToggleCase,
                    label = { Text(if (caseSensitive) "Aa: on" else "Aa: off") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (caseSensitive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ),
                )
                Text(
                    if (matchCount == 0) (if (query.isEmpty()) "Type to search" else "No matches")
                    else "$currentMatch / $matchCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

private fun Modifier.heightAtLeast(): Modifier = this.heightIn(min = 240.dp)

private fun findAllMatches(text: String, query: String, caseSensitive: Boolean): List<IntRange> {
    if (query.isEmpty() || text.isEmpty()) return emptyList()
    val out = mutableListOf<IntRange>()
    val ignore = !caseSensitive
    var i = 0
    val n = text.length
    val q = query.length
    while (i <= n - q) {
        if (text.regionMatches(i, query, 0, q, ignoreCase = ignore)) {
            out.add(IntRange(i, i + q - 1))
            i += q
        } else {
            i++
        }
    }
    return out
}

@Composable
private fun MarkdownToolbar(
    onInsertAround: (String, String) -> Unit,
    onInsertLine: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AssistChip(onClick = { onInsertAround("**", "**") }, label = { Text("B", fontWeight = FontWeight.Bold) })
        AssistChip(onClick = { onInsertAround("*", "*") }, label = { Text("i") })
        AssistChip(onClick = { onInsertAround("~~", "~~") }, label = { Text("S") })
        AssistChip(onClick = { onInsertAround("`", "`") }, label = { Text("</>") })
        AssistChip(onClick = { onInsertLine("# ") }, label = { Text("H1") })
        AssistChip(onClick = { onInsertLine("## ") }, label = { Text("H2") })
        AssistChip(onClick = { onInsertLine("- ") }, label = { Text("•") })
        AssistChip(onClick = { onInsertLine("1. ") }, label = { Text("1.") })
        AssistChip(onClick = { onInsertLine("- [ ] ") }, label = { Text("☐") })
        AssistChip(onClick = { onInsertLine("> ") }, label = { Text("\"") })
        AssistChip(onClick = { onInsertAround("[", "](url)") }, label = { Text("Link") })
    }
}

@Composable
private fun FolderPickerDialog(
    currentFolderId: String?,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { com.notp9194bot.jnotes.ServiceLocator.repo(context) }
    val folders by repo.observeFolders().collectAsState(initial = emptyList())
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onPick(null) }) {
                    Text(if (currentFolderId == null) "✓  No folder" else "No folder")
                }
                folders.forEach { f ->
                    TextButton(onClick = { onPick(f.id) }) {
                        Text(if (currentFolderId == f.id) "✓  ${f.name}" else f.name)
                    }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Create folder") },
                        singleLine = true,
                    )
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            val f = com.notp9194bot.jnotes.data.model.Folder(
                                id = com.notp9194bot.jnotes.data.model.newFolderId(),
                                name = newName.trim(),
                            )
                            scope.launch {
                                repo.upsertFolder(f)
                                onPick(f.id)
                            }
                        }
                    }) { Text("Add") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun toTitleCase(s: String): String {
    if (s.isEmpty()) return s
    val sb = StringBuilder(s.length)
    var capNext = true
    for (c in s) {
        if (c.isWhitespace() || c == '\n' || c == '.' || c == '!' || c == '?') {
            sb.append(c)
            capNext = true
        } else if (capNext) {
            sb.append(c.uppercaseChar())
            capNext = false
        } else {
            sb.append(c.lowercaseChar())
        }
    }
    return sb.toString()
}
