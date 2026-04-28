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
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class TrashPurgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val days = ServiceLocator.settings(ctx).flow.first().purgeDays.coerceAtLeast(1)
            val cutoff = System.currentTimeMillis() - days.toLong() * 24L * 60L * 60L * 1000L
            ServiceLocator.repo(ctx).purgeOlderThan(cutoff)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE = "trash_purge_worker"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }
    }
}
