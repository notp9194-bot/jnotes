package com.notp9194bot.jnotesadmin.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-launches the polling foreground service after device boot or after
 * the admin app is updated, so notifications continue to flow without
 * the user having to open the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                AdminPollingService.start(context.applicationContext)
            }
        }
    }
}
