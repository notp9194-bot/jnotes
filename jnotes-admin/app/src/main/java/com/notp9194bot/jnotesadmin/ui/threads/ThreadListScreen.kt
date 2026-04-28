package com.notp9194bot.jnotesadmin.ui.threads

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

class ThreadsViewModel(app: Application) : AndroidViewModel(app) {
    data class UiState(
        val configured: Boolean = false,
        val loading: Boolean = false,
        val error: String? = null,
        val threads: List<JnotesApi.Thread> = emptyList(),
        val feedback: List<JnotesApi.Feedback> = emptyList(),
    )

    private val prefs = AdminPrefs(getApplication())
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            while (true) {
                val cfg = prefs.flow.firstOrNull()
                if (cfg == null || cfg.serverUrl.isBlank()) {
                    _state.value = UiState(
                        configured = false,
                        error = "Configure the server URL in Settings.",
                    )
                    delay(3000); continue
                }
                val api = JnotesApi(cfg.serverUrl, cfg.adminToken)
                val threads = runCatching { api.listThreads() }.getOrNull()
                val feedback = runCatching { api.listFeedback() }.getOrNull()
                if (threads == null || feedback == null) {
                    _state.value = _state.value.copy(
                        configured = true,
                        loading = false,
                        error = "Cannot reach server (check URL and admin token).",
                    )
                } else {
                    _state.value = UiState(
                        configured = true,
                        loading = false,
                        error = null,
                        threads = threads.sortedByDescending { it.lastTs },
                        feedback = feedback.sortedByDescending { it.ts },
                    )
                }
                delay(4000)
            }
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            val cfg = prefs.flow.firstOrNull() ?: return@launch
            if (cfg.serverUrl.isBlank()) return@launch
            _state.value = _state.value.copy(loading = true)
            val api = JnotesApi(cfg.serverUrl, cfg.adminToken)
            val threads = runCatching { api.listThreads() }.getOrNull() ?: emptyList()
            val feedback = runCatching { api.listFeedback() }.getOrNull() ?: emptyList()
            _state.value = _state.value.copy(
                loading = false,
                threads = threads.sortedByDescending { it.lastTs },
                feedback = feedback.sortedByDescending { it.ts },
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ThreadsViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    onOpenChat: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: ThreadsViewModel = viewModel(factory = ThreadsViewModel.Factory)
    val state by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { vm.start() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("jnotes Admin", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (state.configured) "Live inbox" else "Not connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshNow() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.Chat, null) },
                    text = { Text("Chats (${state.threads.size})") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.Feedback, null) },
                    text = { Text("Feedback (${state.feedback.size})") },
                )
            }
            if (!state.configured) {
                EmptyMessage(state.error ?: "Not configured. Open Settings to connect.")
                return@Column
            }
            if (tab == 0) ThreadList(state.threads, onOpenChat) else FeedbackList(state.feedback)

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
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ThreadList(threads: List<JnotesApi.Thread>, onOpenChat: (String, String) -> Unit) {
    if (threads.isEmpty()) { EmptyMessage("No conversations yet."); return }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(threads, key = { it.userId }) { t ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenChat(t.userId, t.name) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t.name.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                t.name.ifBlank { "Anon" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                formatTime(t.lastTs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (t.lastFrom == "admin") "You: " else "") + t.lastMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (t.unread > 0) {
                                Spacer(Modifier.size(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        t.unread.toString(),
                                        color = MaterialTheme.colorScheme.onTertiary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackList(items: List<JnotesApi.Feedback>) {
    if (items.isEmpty()) { EmptyMessage("No feedback yet."); return }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { f ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            f.name.ifBlank { "Anon" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            formatTime(f.ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(f.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return ""
    val now = System.currentTimeMillis()
    val sameDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).run {
        format(Date(now)) == format(Date(ts))
    }
    val fmt = if (sameDay) "HH:mm" else "MMM d"
    return SimpleDateFormat(fmt, Locale.getDefault()).format(Date(ts))
}
