package com.notp9194bot.jnotes.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.FilterParams
import com.notp9194bot.jnotes.data.model.Folder
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.SmartFilter
import com.notp9194bot.jnotes.data.model.SortBy
import com.notp9194bot.jnotes.data.model.Template
import com.notp9194bot.jnotes.data.model.ViewMode
import com.notp9194bot.jnotes.data.model.newNoteId
import com.notp9194bot.jnotes.domain.Filtering
import com.notp9194bot.jnotes.domain.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val tags: List<String> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val filter: FilterParams = FilterParams(),
    val selectedIds: Set<String> = emptySet(),
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.repo(app)
    private val settingsStore = ServiceLocator.settings(app)

    private val filterFlow = MutableStateFlow(FilterParams())
    private val selectedFlow = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            settingsStore.flow.collect { settings ->
                filterFlow.value = filterFlow.value.copy(sortBy = settings.sortBy)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repo.observeAll(),
        repo.observeFolders(),
        filterFlow,
        selectedFlow,
    ) { notes, folders, filter, selected ->
        HomeUiState(
            notes = Filtering.apply(notes, filter),
            tags = Filtering.allTags(notes),
            folders = folders,
            filter = filter,
            selectedIds = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setView(v: ViewMode) {
        filterFlow.value = filterFlow.value.copy(view = v, smartFilter = null, activeTag = null, activeFolderId = null)
        selectedFlow.value = emptySet()
    }

    fun setSearch(q: String) {
        filterFlow.value = filterFlow.value.copy(search = q)
    }

    fun setSort(s: SortBy) {
        filterFlow.value = filterFlow.value.copy(sortBy = s)
        viewModelScope.launch { settingsStore.setSort(s) }
    }

    fun setSmartFilter(f: SmartFilter?) {
        filterFlow.value = filterFlow.value.copy(smartFilter = f, activeTag = null, activeFolderId = null)
    }

    fun setActiveTag(tag: String?) {
        filterFlow.value = filterFlow.value.copy(activeTag = tag, smartFilter = null, activeFolderId = null)
    }

    fun setActiveFolder(id: String?) {
        filterFlow.value = filterFlow.value.copy(activeFolderId = id, activeTag = null, smartFilter = null)
    }

    fun toggleSelect(id: String) {
        val s = selectedFlow.value.toMutableSet()
        if (!s.add(id)) s.remove(id)
        selectedFlow.value = s
    }

    fun clearSelection() { selectedFlow.value = emptySet() }
    fun selectAll() { selectedFlow.value = uiState.value.notes.map { it.id }.toSet() }

    fun bulkPinToggle() {
        val ids = selectedFlow.value
        viewModelScope.launch {
            for (n in uiState.value.notes.filter { it.id in ids }) repo.pinToggle(n)
            selectedFlow.value = emptySet()
        }
    }

    fun bulkArchive() {
        val ids = selectedFlow.value
        viewModelScope.launch {
            for (n in uiState.value.notes.filter { it.id in ids }) repo.archive(n)
            selectedFlow.value = emptySet()
        }
    }

    fun bulkTrash() {
        val ids = selectedFlow.value
        viewModelScope.launch {
            for (n in uiState.value.notes.filter { it.id in ids }) {
                repo.trash(n)
                ReminderScheduler.cancel(getApplication(), n.id)
            }
            selectedFlow.value = emptySet()
        }
    }

    fun bulkColor(idx: Int) {
        val ids = selectedFlow.value
        viewModelScope.launch {
            for (n in uiState.value.notes.filter { it.id in ids }) repo.setColor(n, idx)
            selectedFlow.value = emptySet()
        }
    }

    fun bulkMoveToFolder(folderId: String?) {
        val ids = selectedFlow.value
        viewModelScope.launch {
            for (n in uiState.value.notes.filter { it.id in ids }) repo.upsert(n.copy(folderId = folderId))
            selectedFlow.value = emptySet()
        }
    }

    fun pinToggleSingle(note: Note) { viewModelScope.launch { repo.pinToggle(note) } }
    fun archiveSingle(note: Note) { viewModelScope.launch { repo.archive(note) } }
    fun unarchiveSingle(note: Note) { viewModelScope.launch { repo.unarchive(note) } }
    fun trashSingle(note: Note) {
        viewModelScope.launch {
            repo.trash(note)
            ReminderScheduler.cancel(getApplication(), note.id)
        }
    }
    fun restoreSingle(note: Note) { viewModelScope.launch { repo.restore(note) } }
    fun deletePermanently(id: String) {
        viewModelScope.launch {
            repo.deletePermanently(id)
            ReminderScheduler.cancel(getApplication(), id)
        }
    }
    fun emptyTrash() { viewModelScope.launch { repo.emptyTrash() } }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.upsertFolder(
                com.notp9194bot.jnotes.data.model.Folder(
                    id = com.notp9194bot.jnotes.data.model.newFolderId(),
                    name = name.trim(),
                ),
            )
        }
    }

    fun deleteFolder(id: String) { viewModelScope.launch { repo.deleteFolder(id) } }

    suspend fun createBlank(): String = repo.newBlank(filterFlow.value.activeFolderId).id

    suspend fun createFromTemplate(t: Template): String {
        val id = newNoteId()
        val items = t.items.map {
            com.notp9194bot.jnotes.data.model.ChecklistItem(id = newNoteId(), text = it, checked = false)
        }
        val note = Note(
            id = id,
            title = t.title,
            body = t.body,
            type = t.type,
            items = items,
            tags = t.tags,
            colorIdx = t.colorIdx,
            folderId = filterFlow.value.activeFolderId,
        )
        repo.upsert(note)
        return id
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                return HomeViewModel(app) as T
            }
        }
    }
}
