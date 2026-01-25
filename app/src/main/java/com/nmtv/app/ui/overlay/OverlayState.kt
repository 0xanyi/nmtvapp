package com.nmtv.app.ui.overlay

/**
 * Represents the current state of the player overlay.
 * Used by PlayerOverlayManager for state machine transitions.
 */
sealed class OverlayState {
    /**
     * Player is hidden (playing normally with no overlays)
     */
    object Hidden : OverlayState()

    /**
     * Loading/buffering state - shows loading spinner
     */
    object Loading : OverlayState()

    /**
     * Paused state - shows play button and pause message
     */
    object Paused : OverlayState()

    /**
     * Error state with retry information
     */
    data class Error(
        val errorType: PlaybackErrorType,
        val message: String,
        val retryAttempt: Int,
        val maxRetries: Int,
        val isRetrying: Boolean
    ) : OverlayState()

    /**
     * Channel banner showing current channel info
     */
    data class ChannelBanner(
        val channelName: String,
        val channelInfo: String
    ) : OverlayState()
}

/**
 * Types of playback errors for better user messaging
 */
enum class PlaybackErrorType {
    NETWORK,        // Network connectivity issues
    STREAM,         // Stream not available or invalid
    CODEC,          // Codec/format not supported
    TIMEOUT,        // Connection timeout
    AUTH,           // Authentication required
    UNKNOWN         // Unknown error
}
