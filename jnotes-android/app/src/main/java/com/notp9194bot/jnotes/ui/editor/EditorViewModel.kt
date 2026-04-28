package com.notp9194bot.jnotes.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.ChecklistItem
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.newNoteId
import com.notp9194bot.jnotes.domain.NoteLinking
import com.notp9194bot.jnotes.domain.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorUiState(
    val note: Note? = null,
    val readingMode: Boolean = false,
    val backlinks: List<Note> = emptyList(),
)

class EditorViewModel(
    private val app: Application,
    handle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val noteId: String = handle["noteId"] ?: ""
    private val repo = ServiceLocator.repo(app)

    private val draft = MutableStateFlow<Note?>(null)
    private val reading = MutableStateFlow(false)
    private val allNotes = repo.observeAll()

    val state: StateFlow<EditorUiState> = combine(
        repo.observeById(noteId),
        draft,
        reading,
        allNotes,
    ) { stored, current, isReading, notes ->
        val note = current ?: stored
        val backlinks = if (note != null) NoteLinking.backlinksFor(note, notes) else emptyList()
        EditorUiState(note = note, readingMode = isReading, backlinks = backlinks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    fun update(updater: (Note) -> Note) {
        val current = draft.value ?: state.value.note ?: return
        val updated = updater(current)
        draft.value = updated
    }

    fun toggleReading() { reading.value = !reading.value }

    fun saveNow() {
        val n = draft.value ?: return
        viewModelScope.launch {
            repo.upsert(n)
            draft.value = null
        }
    }

    fun pinToggle() { update { it.copy(pinned = !it.pinned) } }

    fun setColor(idx: Int) { update { it.copy(colorIdx = idx) } }

    fun setType(type: NoteType) {
        update { current ->
            if (current.type == type) current
            else if (type == NoteType.CHECKLIST) {
                val seed = current.body.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { ChecklistItem(id = newNoteId(), text = it.removePrefix("- ").removePrefix("* "), checked = false) }
                current.copy(type = NoteType.CHECKLIST, items = seed.ifEmpty { listOf(ChecklistItem(id = newNoteId(), text = "", checked = false)) }, body = "")
            } else {
                val text = current.items.joinToString("\n") { (if (it.checked) "- [x] " else "- [ ] ") + it.text }
                current.copy(type = NoteType.TEXT, items = emptyList(), body = if (current.body.isNotEmpty()) current.body else text)
            }
        }
    }

    fun setReminder(
        at: Long,
        title: String,
        body: String,
        recurrence: com.notp9194bot.jnotes.data.model.Recurrence = com.notp9194bot.jnotes.data.model.Recurrence.NONE,
        interval: Int = 1,
    ) {
        update { it.copy(reminderAt = at, recurrence = recurrence, recurrenceInterval = interval.coerceAtLeast(1)) }
        viewModelScope.launch {
            saveNow()
            ReminderScheduler.schedule(app, noteId, title, body, at)
        }
    }

    fun clearReminder() {
        update { it.copy(reminderAt = 0L, recurrence = com.notp9194bot.jnotes.data.model.Recurrence.NONE) }
        ReminderScheduler.cancel(app, noteId)
        viewModelScope.launch { saveNow() }
    }

    fun setFolder(folderId: String?) {
        update { it.copy(folderId = folderId) }
        viewModelScope.launch { saveNow() }
    }

    fun addAttachment(uri: String, kind: String) {
        viewModelScope.launch {
            saveNow()
            repo.addAttachment(noteId, uri, kind)
            update {
                it.copy(
                    attachmentUris = it.attachmentUris + uri,
                    attachmentKinds = it.attachmentKinds + kind,
                )
            }
        }
    }

    fun removeAttachment(uri: String) {
        viewModelScope.launch {
            saveNow()
            repo.removeAttachment(noteId, uri)
            update {
                val idx = it.attachmentUris.indexOf(uri)
                if (idx < 0) it
                else it.copy(
                    attachmentUris = it.attachmentUris.toMutableList().also { l -> l.removeAt(idx) },
                    attachmentKinds = it.attachmentKinds.toMutableList().also { l ->
                        if (idx < l.size) l.removeAt(idx)
                    },
                )
            }
        }
    }

    fun insertAtCursor(text: String) {
        update { it.copy(body = it.body + text) }
    }

    fun archive() {
        update { it.copy(archived = true, pinned = false) }
        viewModelScope.launch { saveNow() }
    }

    fun trash() {
        update { it.copy(trashed = true, trashedAt = System.currentTimeMillis(), pinned = false, archived = false) }
        ReminderScheduler.cancel(app, noteId)
        viewModelScope.launch { saveNow() }
    }

    fun restore() {
        update { it.copy(trashed = false, trashedAt = 0L) }
        viewModelScope.launch { saveNow() }
    }

    fun resolveLinkedTitle(title: String): String? {
        val notes = state.value.let { listOfNotNull(it.note) + it.backlinks }
        val resolved = NoteLinking.resolveByTitle(notes, title)
        if (resolved != null) return resolved.id
        // create a stub note with that title
        val stub = Note(id = newNoteId(), title = title)
        viewModelScope.launch { repo.upsert(stub) }
        return stub.id
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = extras.createSavedStateHandle()
                return EditorViewModel(app, handle) as T
            }
        }
    }
}
