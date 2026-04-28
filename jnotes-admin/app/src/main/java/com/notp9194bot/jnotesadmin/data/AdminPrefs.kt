package com.notp9194bot.jnotesadmin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "admin_prefs")

data class AdminConfig(
    val serverUrl: String = "",
    val adminToken: String = "",
)

class AdminPrefs(private val context: Context) {
    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val adminToken = stringPreferencesKey("admin_token")
    }

    val flow: Flow<AdminConfig> = context.dataStore.data.map {
        AdminConfig(
            serverUrl = it[Keys.serverUrl].orEmpty(),
            adminToken = it[Keys.adminToken].orEmpty(),
        )
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[Keys.serverUrl] = url.trim() }
    }

    suspend fun setAdminToken(token: String) {
        context.dataStore.edit { it[Keys.adminToken] = token.trim() }
    }
}
