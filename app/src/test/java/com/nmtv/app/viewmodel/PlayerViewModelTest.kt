package com.nmtv.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.media3.common.PlaybackException
import com.nmtv.app.ui.overlay.OverlayState
import com.nmtv.app.ui.overlay.PlaybackErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for PlayerViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PlayerViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has loading overlay`() {
        val state = viewModel.uiState.value

        assertTrue(state.overlayState is OverlayState.Loading)
        assertNull(state.currentChannel)
        assertEquals(false, state.isPlaying)
        assertEquals(0, state.retryAttempt)
    }

    @Test
    fun `setChannel with null uses default channel`() {
        viewModel.setChannel(null)

        val channel = viewModel.getCurrentChannel()
        assertNotNull(channel)
        assertEquals("nmtv_uk", channel?.id)
    }

    @Test
    fun `setChannel with valid id sets channel`() {
        viewModel.setChannel("nmtv_classics")

        val channel = viewModel.getCurrentChannel()
        assertNotNull(channel)
        assertEquals("nmtv_classics", channel?.id)
    }

    @Test
    fun `setChannel resets retry attempt`() {
        // Simulate some retries
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))

        assertEquals(2, viewModel.uiState.value.retryAttempt)

        // Set new channel
        viewModel.setChannel("nmtv_classics")

        assertEquals(0, viewModel.uiState.value.retryAttempt)
    }

    @Test
    fun `onPlaying sets hidden state`() {
        viewModel.onPlaying()

        val state = viewModel.uiState.value
        assertTrue(state.overlayState is OverlayState.Hidden)
        assertEquals(true, state.isPlaying)
        assertEquals(0, state.retryAttempt)
    }

    @Test
    fun `onPaused sets paused state`() {
        viewModel.onPlaying() // First start playing
        viewModel.onPaused()

        val state = viewModel.uiState.value
        assertTrue(state.overlayState is OverlayState.Paused)
        assertEquals(false, state.isPlaying)
    }

    @Test
    fun `onPaused does not override error state`() {
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        viewModel.onPaused()

        val state = viewModel.uiState.value
        assertTrue(state.overlayState is OverlayState.Error)
    }

    @Test
    fun `onBuffering sets loading state`() {
        viewModel.onBuffering()

        val state = viewModel.uiState.value
        assertTrue(state.overlayState is OverlayState.Loading)
        assertEquals(false, state.isPlaying)
    }

    @Test
    fun `onError sets error state with correct type`() {
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))

        val state = viewModel.uiState.value
        assertTrue(state.overlayState is OverlayState.Error)

        val errorState = state.overlayState as OverlayState.Error
        assertEquals(PlaybackErrorType.NETWORK, errorState.errorType)
        assertEquals(1, errorState.retryAttempt)
        assertEquals(true, errorState.isRetrying)
    }

    @Test
    fun `onError increments retry count`() {
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertEquals(1, viewModel.uiState.value.retryAttempt)

        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertEquals(2, viewModel.uiState.value.retryAttempt)

        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertEquals(3, viewModel.uiState.value.retryAttempt)
    }

    @Test
    fun `onError stops retrying after max retries`() {
        // Exhaust all retries
        repeat(5) {
            viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        }

        val state = viewModel.uiState.value
        assertTrue(state.overlayState is OverlayState.Error)

        val errorState = state.overlayState as OverlayState.Error
        assertEquals(false, errorState.isRetrying)
        assertEquals(5, errorState.retryAttempt)
    }

    @Test
    fun `switchToNextChannel returns next channel`() {
        viewModel.setChannel("nmtv_uk")

        val nextChannel = viewModel.switchToNextChannel()

        assertNotNull(nextChannel)
        assertEquals("nmtv_classics", nextChannel?.id)
        assertEquals("nmtv_classics", viewModel.getCurrentChannel()?.id)
    }

    @Test
    fun `switchToPreviousChannel returns previous channel`() {
        viewModel.setChannel("nmtv_classics")

        val prevChannel = viewModel.switchToPreviousChannel()

        assertNotNull(prevChannel)
        assertEquals("nmtv_uk", prevChannel?.id)
        assertEquals("nmtv_uk", viewModel.getCurrentChannel()?.id)
    }

    @Test
    fun `channel switch resets retry counter`() {
        viewModel.setChannel("nmtv_uk")
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))

        assertEquals(2, viewModel.uiState.value.retryAttempt)

        viewModel.switchToNextChannel()

        assertEquals(0, viewModel.uiState.value.retryAttempt)
    }

    @Test
    fun `resetRetryCounter resets to zero`() {
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        viewModel.onError(createMockError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))

        assertEquals(2, viewModel.uiState.value.retryAttempt)

        viewModel.resetRetryCounter()

        assertEquals(0, viewModel.uiState.value.retryAttempt)
    }

    private fun createMockError(errorCode: Int): PlaybackException {
        return PlaybackException("Test error", null, errorCode)
    }
}
