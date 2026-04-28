package com.notp9194bot.jnotes.domain.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.util.JsonBackup
import com.notp9194bot.jnotes.util.WebDavSync
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val s = ServiceLocator.settings(ctx).flow.first()
        val url = s.webdavUrl.orEmpty()
        if (url.isBlank()) return Result.success()

        return try {
            val notes = ServiceLocator.repo(ctx).observeAll().first()
            val text = JsonBackup.encode(notes)
            val ok = WebDavSync.put(
                url = "${url.trimEnd('/')}/jnotes-backup.json",
                user = s.webdavUser.orEmpty(),
                pass = s.webdavPass.orEmpty(),
                bytes = text.toByteArray(Charsets.UTF_8),
            )
            if (ok) {
                ServiceLocator.settings(ctx).setLastSync(System.currentTimeMillis())
                Result.success()
            } else Result.retry()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE = "webdav_sync_worker"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE)
        }
    }
}
