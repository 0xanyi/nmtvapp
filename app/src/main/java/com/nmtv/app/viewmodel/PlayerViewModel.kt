package com.nmtv.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository
import com.nmtv.app.ui.overlay.OverlayState
import com.nmtv.app.ui.overlay.PlaybackErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the main player activity.
 * Manages playback state and channel switching.
 */
class PlayerViewModel : ViewModel() {

    private val channelRepository: ChannelRepository = LocalChannelRepository()

    /**
     * Represents the player UI state
     */
    data class PlayerUiState(
        val currentChannel: Channel? = null,
        val overlayState: OverlayState = OverlayState.Loading,
        val isPlaying: Boolean = false,
        val retryAttempt: Int = 0,
        val maxRetries: Int = 5
    )

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /**
     * Set the current channel by ID
     */
    fun setChannel(channelId: String?) {
        val channel = if (channelId != null) {
            channelRepository.getChannelById(channelId)
        } else {
            channelRepository.getDefaultChannel()
        }

        _uiState.value = _uiState.value.copy(
            currentChannel = channel,
            retryAttempt = 0
        )
    }

    /**
     * Get current channel
     */
    fun getCurrentChannel(): Channel? = _uiState.value.currentChannel

    /**
     * Switch to next channel
     */
    fun switchToNextChannel(): Channel? {
        val current = _uiState.value.currentChannel ?: return null
        val next = channelRepository.getNextChannel(current.id) ?: return null

        _uiState.value = _uiState.value.copy(
            currentChannel = next,
            retryAttempt = 0,
            overlayState = OverlayState.Loading
        )
        return next
    }

    /**
     * Switch to previous channel
     */
    fun switchToPreviousChannel(): Channel? {
        val current = _uiState.value.currentChannel ?: return null
        val previous = channelRepository.getPreviousChannel(current.id) ?: return null

        _uiState.value = _uiState.value.copy(
            currentChannel = previous,
            retryAttempt = 0,
            overlayState = OverlayState.Loading
        )
        return previous
    }

    /**
     * Update overlay state based on playback state
     */
    fun onPlaybackStateChanged(playbackState: Int, playWhenReady: Boolean) {
        val newOverlayState = when (playbackState) {
            Player.STATE_BUFFERING -> OverlayState.Loading
            Player.STATE_READY -> {
                if (playWhenReady) OverlayState.Hidden else OverlayState.Paused
            }
            Player.STATE_ENDED -> OverlayState.Hidden
            else -> _uiState.value.overlayState
        }

        _uiState.value = _uiState.value.copy(
            overlayState = newOverlayState,
            isPlaying = playbackState == Player.STATE_READY && playWhenReady
        )
    }

    /**
     * Handle playback started
     */
    fun onPlaying() {
        _uiState.value = _uiState.value.copy(
            overlayState = OverlayState.Hidden,
            isPlaying = true,
            retryAttempt = 0
        )
    }

    /**
     * Handle playback paused
     */
    fun onPaused() {
        val currentState = _uiState.value.overlayState
        // Don't override error or loading states
        if (currentState !is OverlayState.Error && currentState !is OverlayState.Loading) {
            _uiState.value = _uiState.value.copy(
                overlayState = OverlayState.Paused,
                isPlaying = false
            )
        }
    }

    /**
     * Handle buffering
     */
    fun onBuffering() {
        _uiState.value = _uiState.value.copy(
            overlayState = OverlayState.Loading,
            isPlaying = false
        )
    }

    /**
     * Handle playback error
     */
    fun onError(error: PlaybackException) {
        val state = _uiState.value
        val newRetryAttempt = state.retryAttempt + 1
        val isRetrying = newRetryAttempt < state.maxRetries

        val errorType = mapErrorType(error)
        val errorMessage = buildErrorMessage(errorType, newRetryAttempt, state.maxRetries, isRetrying)

        _uiState.value = state.copy(
            overlayState = OverlayState.Error(
                errorType = errorType,
                message = errorMessage,
                retryAttempt = newRetryAttempt,
                maxRetries = state.maxRetries,
                isRetrying = isRetrying
            ),
            retryAttempt = newRetryAttempt,
            isPlaying = false
        )
    }

    /**
     * Reset retry counter (call on channel switch or manual retry)
     */
    fun resetRetryCounter() {
        _uiState.value = _uiState.value.copy(retryAttempt = 0)
    }

    private fun mapErrorType(error: PlaybackException): PlaybackErrorType {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackErrorType.NETWORK

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackErrorType.STREAM

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> PlaybackErrorType.CODEC

            PlaybackException.ERROR_CODE_TIMEOUT -> PlaybackErrorType.TIMEOUT

            PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> PlaybackErrorType.AUTH

            else -> PlaybackErrorType.UNKNOWN
        }
    }

    private fun buildErrorMessage(
        errorType: PlaybackErrorType,
        retryAttempt: Int,
        maxRetries: Int,
        isRetrying: Boolean
    ): String {
        val errorDescription = when (errorType) {
            PlaybackErrorType.NETWORK -> "Network connection issue"
            PlaybackErrorType.STREAM -> "Stream unavailable"
            PlaybackErrorType.CODEC -> "Video format not supported"
            PlaybackErrorType.TIMEOUT -> "Connection timed out"
            PlaybackErrorType.AUTH -> "Authentication required"
            PlaybackErrorType.UNKNOWN -> "Playback error"
        }

        return if (isRetrying) {
            "$errorDescription\nRetrying... ($retryAttempt/$maxRetries)"
        } else {
            "$errorDescription\nUnable to connect. Please check your internet connection."
        }
    }
}
