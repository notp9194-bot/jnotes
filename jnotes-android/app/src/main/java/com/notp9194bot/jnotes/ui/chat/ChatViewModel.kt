package com.notp9194bot.jnotes.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.remote.JnotesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    data class UiState(
        val configured: Boolean = false,
        val loading: Boolean = false,
        val sending: Boolean = false,
        val error: String? = null,
        val info: String? = null,
        val messages: List<JnotesApi.ChatMessage> = emptyList(),
    )

    private val store = ServiceLocator.settings(getApplication())
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pollingStarted = false

    fun start() {
        if (pollingStarted) return
        pollingStarted = true
        viewModelScope.launch {
            val s = store.flow.firstOrNull() ?: return@launch
            val url = s.serverUrl
            if (url.isBlank()) {
                _state.value = _state.value.copy(
                    configured = false,
                    error = "Set the Server URL in Settings to use chat.",
                )
                return@launch
            }
            val userId = store.ensureUserId()
            val name = s.displayName.ifBlank { "Anon" }
            val api = JnotesApi(url)
            runCatching { api.register(userId, name) }
            _state.value = _state.value.copy(configured = true, loading = true)
            // Initial fetch + long polling loop
            while (true) {
                val msgs = runCatching { api.getMessages(userId) }.getOrNull()
                if (msgs != null) {
                    _state.value = _state.value.copy(loading = false, error = null, messages = msgs)
                    runCatching { api.markRead(userId, who = "user") }
                } else if (_state.value.messages.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = "Cannot reach the server.")
                }
                delay(3500)
            }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val s = store.flow.firstOrNull() ?: return@launch
            val url = s.serverUrl.takeIf { it.isNotBlank() } ?: return@launch
            val userId = store.ensureUserId()
            val name = s.displayName.ifBlank { "Anon" }
            val api = JnotesApi(url)
            _state.value = _state.value.copy(sending = true)
            val msg = runCatching { api.sendMessage(userId, from = "user", text = trimmed, name = name) }
                .getOrNull()
            if (msg != null) {
                _state.value = _state.value.copy(
                    sending = false,
                    messages = _state.value.messages + msg,
                    error = null,
                )
            } else {
                _state.value = _state.value.copy(sending = false, error = "Failed to send. Check connection.")
            }
        }
    }

    fun submitFeedback(text: String, onDone: (Boolean) -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) { onDone(false); return }
        viewModelScope.launch {
            val s = store.flow.firstOrNull() ?: return@launch onDone(false)
            val url = s.serverUrl.takeIf { it.isNotBlank() } ?: return@launch onDone(false)
            val userId = store.ensureUserId()
            val name = s.displayName.ifBlank { "Anon" }
            val ok = runCatching { JnotesApi(url).sendFeedback(userId, name, trimmed) }
                .getOrDefault(false)
            onDone(ok)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ChatViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}
