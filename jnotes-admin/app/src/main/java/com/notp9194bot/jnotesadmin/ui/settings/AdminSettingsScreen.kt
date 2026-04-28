package com.notp9194bot.jnotesadmin.ui.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.notp9194bot.jnotesadmin.data.AdminConfig
import com.notp9194bot.jnotesadmin.data.AdminPrefs
import com.notp9194bot.jnotesadmin.data.JnotesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = AdminPrefs(getApplication())
    val config: StateFlow<AdminConfig> = prefs.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminConfig(),
    )

    private val _pushedUrl = MutableStateFlow("")
    val pushedUrl: StateFlow<String> = _pushedUrl

    fun loadPushedUrl() {
        viewModelScope.launch {
            val cfg = config.value
            if (cfg.serverUrl.isBlank()) return@launch
            val u = runCatching { JnotesApi(cfg.serverUrl, cfg.adminToken).fetchPushedUrl() }.getOrNull()
            _pushedUrl.value = u ?: ""
        }
    }

    fun pushUrl(broadcastUrl: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cfg = config.value
            if (cfg.serverUrl.isBlank() || cfg.adminToken.isBlank()) return@launch onResult(false)
            val ok = runCatching {
                JnotesApi(cfg.serverUrl, cfg.adminToken).pushServerUrl(broadcastUrl)
            }.getOrDefault(false)
            if (ok) _pushedUrl.value = broadcastUrl
            onResult(ok)
        }
    }

    fun save(serverUrl: String, token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            prefs.setServerUrl(serverUrl)
            prefs.setAdminToken(token)
            val ok = if (serverUrl.isNotBlank()) {
                runCatching { JnotesApi(serverUrl, token).ping() }.getOrDefault(false)
            } else false
            onResult(ok)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AdminSettingsViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(onBack: () -> Unit) {
    val vm: AdminSettingsViewModel = viewModel(factory = AdminSettingsViewModel.Factory)
    val cfg by vm.config.collectAsState()
    val pushed by vm.pushedUrl.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var url by remember(cfg.serverUrl) { mutableStateOf(cfg.serverUrl) }
    var token by remember(cfg.adminToken) { mutableStateOf(cfg.adminToken) }
    var broadcast by remember(pushed, cfg.serverUrl) {
        mutableStateOf(pushed.ifBlank { cfg.serverUrl })
    }

    LaunchedEffect(cfg.serverUrl, cfg.adminToken) { vm.loadPushedUrl() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Server connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Point this admin app at the same backend the user app uses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://jnotes-ypx2.onrender.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Admin token") },
                        placeholder = { Text("X-Admin-Token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            vm.save(url, token) { ok ->
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (ok) "Connected ✓" else "Saved, but server is unreachable.",
                                    )
                                }
                            }
                        }) { Text("Save & test") }
                        OutlinedButton(onClick = {
                            url = ""; token = ""
                            vm.save("", "") { _ ->
                                scope.launch { snackbar.showSnackbar("Cleared") }
                            }
                        }) { Text("Clear") }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Push server URL to user apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "User apps poll this value and switch to the URL you set here. " +
                            "Use it to migrate everyone to a new backend without updating the APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    if (pushed.isNotBlank()) {
                        Text(
                            "Currently broadcast: $pushed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        )
                    }
                    OutlinedTextField(
                        value = broadcast,
                        onValueChange = { broadcast = it },
                        label = { Text("URL to broadcast") },
                        placeholder = { Text("https://jnotes-ypx2.onrender.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            vm.pushUrl(broadcast.trim()) { ok ->
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (ok) "Pushed to all user apps ✓" else "Push failed (check token).",
                                    )
                                }
                            }
                        }) { Text("Push to all users") }
                        OutlinedButton(onClick = { vm.loadPushedUrl() }) { Text("Refresh") }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How it works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Run the jnotes-backend Node.js relay on a machine reachable by both the user device and this admin device. " +
                            "Set the same URL in both apps. The admin token must match the ADMIN_TOKEN env var on the server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}
