package com.notp9194bot.jnotesadmin.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    data class UiState(
        val configured: Boolean = false,
        val loading: Boolean = false,
        val error: String? = null,
        val data: JnotesApi.Dashboard? = null,
    )

    private val prefs = AdminPrefs(getApplication())
    private val _state = MutableStateFlow(UiState(loading = true))
    val state: StateFlow<UiState> = _state

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(8000)
            }
        }
    }

    suspend fun refresh() {
        val cfg = prefs.flow.firstOrNull()
        if (cfg == null || cfg.serverUrl.isBlank() || cfg.adminToken.isBlank()) {
            _state.value = UiState(
                configured = false,
                loading = false,
                error = "Configure the server URL and admin token in Settings.",
            )
            return
        }
        val api = JnotesApi(cfg.serverUrl, cfg.adminToken)
        val dash = runCatching { api.fetchDashboard() }.getOrNull()
        _state.value = if (dash == null) {
            _state.value.copy(
                configured = true,
                loading = false,
                error = "Cannot reach server (check URL and admin token).",
            )
        } else {
            UiState(configured = true, loading = false, error = null, data = dash)
        }
    }

    fun setBlock(userId: String, blockChat: Boolean? = null, blockFeedback: Boolean? = null, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cfg = prefs.flow.firstOrNull() ?: return@launch onResult(false)
            val api = JnotesApi(cfg.serverUrl, cfg.adminToken)
            val res = runCatching { api.setUserBlock(userId, blockChat, blockFeedback) }.getOrNull()
            if (res != null) refresh()
            onResult(res != null)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DashboardViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.start() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { vm.refresh() } }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val data = state.data
            if (data == null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        state.error ?: "Loading…",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    StatGrid(data)
                    if (data.serverUrl.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Broadcast server URL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                                Text(
                                    data.serverUrl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Users (${data.users.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
                items(data.users, key = { it.userId }) { u ->
                    UserCard(
                        u = u,
                        onOpenChat = { onOpenChat(u.userId, u.name) },
                        onToggleChat = {
                            vm.setBlock(u.userId, blockChat = !u.blockChat) { ok ->
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (!ok) "Failed to update."
                                        else if (!u.blockChat) "${u.name} can no longer message you."
                                        else "${u.name} can message you again.",
                                    )
                                }
                            }
                        },
                        onToggleFeedback = {
                            vm.setBlock(u.userId, blockFeedback = !u.blockFeedback) { ok ->
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (!ok) "Failed to update."
                                        else if (!u.blockFeedback) "${u.name} can no longer send feedback."
                                        else "${u.name} can send feedback again.",
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatGrid(d: JnotesApi.Dashboard) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile("Users", d.totalUsers.toString(), Modifier.weight(1f))
        StatTile("Active 24h", d.activeToday.toString(), Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile("Messages", d.totalMessages.toString(), Modifier.weight(1f))
        StatTile("Feedback", d.totalFeedback.toString(), Modifier.weight(1f))
        StatTile("Unread", d.totalUnreadFromUsers.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun UserCard(
    u: JnotesApi.DashboardUser,
    onOpenChat: () -> Unit,
    onToggleChat: () -> Unit,
    onToggleFeedback: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenChat),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        u.name.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        u.name.ifBlank { "Anon" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Last seen ${formatTime(u.lastSeen)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
                if (u.unread > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            u.unread.toString(),
                            color = MaterialTheme.colorScheme.onTertiary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricChip(label = "Msgs", value = u.messageCount)
                MetricChip(label = "Feedback", value = u.feedbackCount)
                MetricChip(label = "Joined", value = formatTime(u.createdAt))
            }

            if (u.lastMessage.isNotBlank()) {
                Text(
                    "“${u.lastMessage}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onToggleChat,
                    label = { Text(if (u.blockChat) "Chat blocked" else "Chat allowed") },
                    leadingIcon = {
                        Icon(
                            if (u.blockChat) Icons.Outlined.Block else Icons.Outlined.Chat,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (u.blockChat) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
                AssistChip(
                    onClick = onToggleFeedback,
                    label = { Text(if (u.blockFeedback) "Feedback blocked" else "Feedback allowed") },
                    leadingIcon = {
                        Icon(
                            if (u.blockFeedback) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (u.blockFeedback) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: Any) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            "$label: $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return "—"
    val now = System.currentTimeMillis()
    val sameDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).run {
        format(Date(now)) == format(Date(ts))
    }
    val fmt = if (sameDay) "HH:mm" else "MMM d"
    return SimpleDateFormat(fmt, Locale.getDefault()).format(Date(ts))
}
