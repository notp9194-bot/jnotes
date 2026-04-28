package com.notp9194bot.jnotesadmin.ui.chat

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.notp9194bot.jnotesadmin.data.AdminPrefs
import com.notp9194bot.jnotesadmin.data.JnotesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminChatViewModel(app: Application) : AndroidViewModel(app) {
    data class UiState(
        val configured: Boolean = false,
        val loading: Boolean = false,
        val sending: Boolean = false,
        val error: String? = null,
        val messages: List<JnotesApi.ChatMessage> = emptyList(),
    )

    private val prefs = AdminPrefs(getApplication())
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var started = false

    fun start(userId: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            val cfg = prefs.flow.firstOrNull()
            if (cfg == null || cfg.serverUrl.isBlank()) {
                _state.value = UiState(configured = false, error = "Configure server URL first.")
                return@launch
            }
            val api = JnotesApi(cfg.serverUrl, cfg.adminToken)
            _state.value = _state.value.copy(configured = true, loading = true)
            while (true) {
                val msgs = runCatching { api.getMessages(userId) }.getOrNull()
                if (msgs != null) {
                    _state.value = _state.value.copy(loading = false, error = null, messages = msgs)
                    runCatching { api.markRead(userId) }
                } else if (_state.value.messages.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = "Cannot reach server.")
                }
                delay(3500)
            }
        }
    }

    fun send(userId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val cfg = prefs.flow.firstOrNull() ?: return@launch
            if (cfg.serverUrl.isBlank()) return@launch
            val api = JnotesApi(cfg.serverUrl, cfg.adminToken)
            _state.value = _state.value.copy(sending = true)
            val msg = runCatching { api.reply(userId, trimmed) }.getOrNull()
            if (msg != null) {
                _state.value = _state.value.copy(
                    sending = false,
                    error = null,
                    messages = _state.value.messages + msg,
                )
            } else {
                _state.value = _state.value.copy(sending = false, error = "Send failed.")
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AdminChatViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatScreen(userId: String, userName: String, onBack: () -> Unit) {
    val vm: AdminChatViewModel = viewModel(factory = AdminChatViewModel.Factory)
    val state by vm.state.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(userId) { vm.start(userId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(userName.ifBlank { "User" }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "ID: ${userId.take(10)}…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Reply to ${userName.ifBlank { "user" }}…") },
                    modifier = Modifier.weight(1f),
                    enabled = state.configured && !state.sending,
                    maxLines = 4,
                )
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            vm.send(userId, draft)
                            draft = ""
                        }
                    },
                    enabled = state.configured && !state.sending && draft.isNotBlank(),
                ) { Icon(Icons.Outlined.Send, contentDescription = "Send") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!state.configured) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        state.error ?: "Configure server URL first.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                return@Column
            }

            if (state.loading && state.messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    Bubble(msg = msg, isMine = msg.from == "admin")
                }
            }

            state.error?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Bubble(msg: JnotesApi.ChatMessage, isMine: Boolean) {
    val bg = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )
    val time = remember(msg.ts) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.ts))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(msg.text, color = fg, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                time,
                color = fg.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
