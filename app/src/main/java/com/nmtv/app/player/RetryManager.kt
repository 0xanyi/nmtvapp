package com.nmtv.app.player

import android.os.Handler
import android.os.Looper
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages retry logic with exponential backoff for stream reconnection.
 *
 * @property maxRetries Maximum number of retry attempts (default: 5)
 * @property initialDelayMs Initial delay in milliseconds (default: 1000ms = 1s)
 * @property maxDelayMs Maximum delay cap in milliseconds (default: 30000ms = 30s)
 */
class RetryManager(
    private val maxRetries: Int = 5,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000
) {
    private val handler = Handler(Looper.getMainLooper())
    private var currentRetryRunnable: Runnable? = null

    /**
     * Schedule a retry with exponential backoff.
     *
     * @param attempt Current attempt number (0-based)
     * @param action The action to execute after the delay
     */
    fun scheduleRetry(attempt: Int, action: () -> Unit) {
        cancel() // Cancel any existing retry

        if (attempt >= maxRetries) {
            // Max retries exceeded, don't schedule
            return
        }

        // Calculate delay with exponential backoff: initialDelay * 2^attempt
        val delay = min(
            initialDelayMs * 2.0.pow(attempt.toDouble()).toLong(),
            maxDelayMs
        )

        currentRetryRunnable = Runnable {
            action()
            currentRetryRunnable = null
        }

        handler.postDelayed(currentRetryRunnable!!, delay)
    }

    /**
     * Reset the retry manager state.
     */
    fun reset() {
        cancel()
    }

    /**
     * Cancel any pending retry.
     */
    fun cancel() {
        currentRetryRunnable?.let {
            handler.removeCallbacks(it)
            currentRetryRunnable = null
        }
    }
}