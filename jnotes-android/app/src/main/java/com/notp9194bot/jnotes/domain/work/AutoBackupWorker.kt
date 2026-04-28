package com.notp9194bot.jnotes.domain.work

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.util.JsonBackup
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val s = ServiceLocator.settings(ctx).flow.first()
            if (!s.autoBackupEnabled || s.autoBackupFolderUri.isNullOrBlank()) return Result.success()

            val folder = DocumentFile.fromTreeUri(ctx, Uri.parse(s.autoBackupFolderUri))
                ?: return Result.failure()
            val notes = ServiceLocator.repo(ctx).observeAll().first()
            val text = JsonBackup.encode(notes)

            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val file = folder.createFile("application/json", "jnotes-backup-$ts.json")
                ?: return Result.failure()
            ctx.contentResolver.openOutputStream(file.uri)?.use {
                it.write(text.toByteArray(Charsets.UTF_8))
            }

            // Keep only the 14 most recent backups
            val existing = folder.listFiles()
                .filter { it.name?.startsWith("jnotes-backup-") == true }
                .sortedByDescending { it.lastModified() }
            existing.drop(14).forEach { it.delete() }

            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE = "auto_backup_worker"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
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
