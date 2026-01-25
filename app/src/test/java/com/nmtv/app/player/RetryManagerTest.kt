package com.nmtv.app.player

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for RetryManager
 */
@RunWith(RobolectricTestRunner::class)
class RetryManagerTest {

    private lateinit var retryManager: RetryManager

    @Before
    fun setup() {
        retryManager = RetryManager(
            maxRetries = 3,
            initialDelayMs = 100,
            maxDelayMs = 1000
        )
    }

    @Test
    fun `scheduleRetry executes action after delay`() {
        val executed = AtomicInteger(0)

        retryManager.scheduleRetry(0) {
            executed.incrementAndGet()
        }

        // Initial delay is 100ms for attempt 0
        // Advance the looper to execute pending tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(1, executed.get())
    }

    @Test
    fun `scheduleRetry does not execute when max retries exceeded`() {
        val executed = AtomicInteger(0)

        // Attempt 3 with maxRetries=3 should not schedule
        retryManager.scheduleRetry(3) {
            executed.incrementAndGet()
        }

        // Run all pending tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(0, executed.get())
    }

    @Test
    fun `cancel stops pending retry`() {
        val executed = AtomicInteger(0)

        retryManager.scheduleRetry(0) {
            executed.incrementAndGet()
        }

        // Cancel immediately
        retryManager.cancel()

        // Run all pending tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(0, executed.get())
    }

    @Test
    fun `reset cancels pending retry`() {
        val executed = AtomicInteger(0)

        retryManager.scheduleRetry(0) {
            executed.incrementAndGet()
        }

        // Reset immediately
        retryManager.reset()

        // Run all pending tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(0, executed.get())
    }

    @Test
    fun `new scheduleRetry cancels previous`() {
        val firstExecuted = AtomicInteger(0)
        val secondExecuted = AtomicInteger(0)

        retryManager.scheduleRetry(0) {
            firstExecuted.incrementAndGet()
        }

        // Schedule another retry immediately - should cancel the first
        retryManager.scheduleRetry(0) {
            secondExecuted.incrementAndGet()
        }

        // Run all pending tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Only the second one should have executed
        assertEquals(0, firstExecuted.get())
        assertEquals(1, secondExecuted.get())
    }

    @Test
    fun `exponential backoff increases delay`() {
        val executed = AtomicInteger(0)

        // Higher attempt = longer delay
        // delay = 100 * 2^2 = 400ms for attempt 2
        retryManager.scheduleRetry(2) {
            executed.incrementAndGet()
        }

        // Advance by 200ms - should not have executed yet
        ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(0, executed.get())

        // Run all delayed tasks to complete
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals(1, executed.get())
    }

    @Test
    fun `multiple retries can be scheduled sequentially`() {
        val executionOrder = mutableListOf<Int>()

        // Schedule first retry
        retryManager.scheduleRetry(0) {
            executionOrder.add(1)
            // Schedule another after first completes
            retryManager.scheduleRetry(0) {
                executionOrder.add(2)
            }
        }

        // Run all pending tasks (including nested)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(listOf(1, 2), executionOrder)
    }

    @Test
    fun `max delay caps exponential growth`() {
        val executed = AtomicInteger(0)

        // Create manager with very short delays for testing
        val manager = RetryManager(
            maxRetries = 10,
            initialDelayMs = 100,
            maxDelayMs = 200 // Cap at 200ms
        )

        // Attempt 5 would be 100 * 2^5 = 3200ms, but capped at 200ms
        manager.scheduleRetry(5) {
            executed.incrementAndGet()
        }

        // Run all delayed tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertEquals(1, executed.get())
    }
}
