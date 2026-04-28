package com.notp9194bot.jnotes.data.remote

import android.content.Context
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Helpers that synchronise the user app's local settings with the relay
 * server so the admin app can push configuration centrally:
 *
 *  • `pullServerUrl(context)`  — asks the currently-configured relay for
 *    its broadcast `serverUrl` and updates local storage if it changed.
 *  • `seedDeviceName(context)` — on first launch, sets the display name
 *    to the device's local IPv4 address (so the admin sees something
 *    useful in the user list).
 *  • `bootstrap(context)`      — runs both, in order, with safe defaults.
 */
object ConfigSync {

    /** Default discovery URL used to fetch the broadcast server URL. */
    const val DEFAULT_URL = "https://jnotes-ypx2.onrender.com"

    suspend fun seedDeviceName(context: Context) = withContext(Dispatchers.IO) {
        val store = ServiceLocator.settings(context)
        val current = store.flow.firstOrNull()?.displayName.orEmpty()
        if (current.isBlank()) {
            store.setDisplayName(NetworkUtils.localIpv4())
        }
    }

    suspend fun pullServerUrl(context: Context) = withContext(Dispatchers.IO) {
        val store = ServiceLocator.settings(context)
        val s = store.flow.firstOrNull() ?: return@withContext
        val discoveryUrl = s.serverUrl.takeIf { it.isNotBlank() } ?: DEFAULT_URL
        val pushed = runCatching { JnotesApi(discoveryUrl).fetchConfig() }.getOrNull()
        if (!pushed.isNullOrBlank() && pushed != s.serverUrl) {
            store.setServerUrl(pushed)
        }
    }

    suspend fun bootstrap(context: Context) {
        seedDeviceName(context)
        pullServerUrl(context)
    }
}
