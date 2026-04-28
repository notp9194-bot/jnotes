package com.notp9194bot.jnotes.ui.lock

/**
 * Tiny process-wide helper that records when the app was last sent to the
 * background. AppNavHost reads this on resume to decide whether to re-lock
 * (based on the user's autoLockSeconds / lockOnExit settings).
 */
object LockManager {

    @Volatile private var backgroundedAt: Long = 0L

    fun markBackgrounded() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun consumeBackgroundedAt(): Long {
        val v = backgroundedAt
        backgroundedAt = 0L
        return v
    }
}
