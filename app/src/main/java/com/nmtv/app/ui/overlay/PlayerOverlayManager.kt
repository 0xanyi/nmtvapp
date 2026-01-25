package com.nmtv.app.ui.overlay

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.PlaybackException

/**
 * Manages player overlay states with smooth transitions.
 * Implements a state machine for overlay management.
 *
 * State transitions:
 * - HIDDEN -> LOADING (on buffering start)
 * - LOADING -> HIDDEN (on playback start)
 * - HIDDEN -> PAUSED (on pause)
 * - PAUSED -> HIDDEN (on resume)
 * - ANY -> ERROR (on error)
 * - ERROR -> LOADING (on retry)
 * - ANY -> CHANNEL_BANNER (on channel switch, auto-hides)
 */
class PlayerOverlayManager(
    private val loadingOverlay: FrameLayout,
    private val pauseOverlay: FrameLayout,
    private val errorOverlay: FrameLayout,
    private val errorMessage: TextView,
    private val errorProgressBar: ProgressBar,
    private val channelBanner: FrameLayout,
    private val channelBannerTitle: TextView,
    private val channelBannerInfo: TextView
) {
    private var currentState: OverlayState = OverlayState.Hidden
    private val handler = Handler(Looper.getMainLooper())
    private var bannerHideRunnable: Runnable? = null

    companion object {
        private const val TAG = "PlayerOverlayManager"
        private const val FADE_DURATION = 200L
        private const val BANNER_DISPLAY_TIME = 3000L
    }

    /**
     * Get the current overlay state
     */
    fun getCurrentState(): OverlayState = currentState

    /**
     * Transition to loading state
     */
    fun showLoading() {
        if (currentState is OverlayState.Loading) return

        Log.d(TAG, "Transition: ${currentState::class.simpleName} -> Loading")
        currentState = OverlayState.Loading

        fadeOut(pauseOverlay)
        fadeOut(errorOverlay)
        fadeIn(loadingOverlay)
    }

    /**
     * Transition to hidden state (playing)
     */
    fun hide() {
        if (currentState is OverlayState.Hidden) return

        Log.d(TAG, "Transition: ${currentState::class.simpleName} -> Hidden")
        currentState = OverlayState.Hidden

        fadeOut(loadingOverlay)
        fadeOut(pauseOverlay)
        fadeOut(errorOverlay)
    }

    /**
     * Transition to paused state
     */
    fun showPaused() {
        // Don't show pause if we're in loading or error state
        if (currentState is OverlayState.Loading || currentState is OverlayState.Error) {
            Log.d(TAG, "Ignoring pause overlay due to current state: ${currentState::class.simpleName}")
            return
        }

        Log.d(TAG, "Transition: ${currentState::class.simpleName} -> Paused")
        currentState = OverlayState.Paused

        fadeOut(loadingOverlay)
        fadeOut(errorOverlay)
        fadeIn(pauseOverlay)
    }

    /**
     * Transition to error state with retry information
     */
    fun showError(
        error: PlaybackException,
        retryAttempt: Int,
        maxRetries: Int,
        isRetrying: Boolean
    ) {
        val errorType = mapErrorType(error)
        val message = buildErrorMessage(errorType, retryAttempt, maxRetries, isRetrying)

        Log.d(TAG, "Transition: ${currentState::class.simpleName} -> Error (type=$errorType, retry=$retryAttempt/$maxRetries)")
        currentState = OverlayState.Error(errorType, message, retryAttempt, maxRetries, isRetrying)

        fadeOut(loadingOverlay)
        fadeOut(pauseOverlay)

        // Update error UI
        errorMessage.text = message
        errorProgressBar.visibility = if (isRetrying) View.VISIBLE else View.GONE

        fadeIn(errorOverlay)
    }

    /**
     * Show channel banner with auto-hide
     */
    fun showChannelBanner(channelName: String, channelInfo: String = "Now Playing: Live Stream") {
        Log.d(TAG, "Showing channel banner: $channelName")

        // Cancel any pending hide
        bannerHideRunnable?.let { handler.removeCallbacks(it) }

        // Update banner content
        channelBannerTitle.text = channelName
        channelBannerInfo.text = channelInfo

        // Show with fade
        fadeIn(channelBanner, targetAlpha = 0.9f)

        // Schedule auto-hide
        bannerHideRunnable = Runnable { hideChannelBanner() }
        handler.postDelayed(bannerHideRunnable!!, BANNER_DISPLAY_TIME)
    }

    /**
     * Hide channel banner
     */
    fun hideChannelBanner() {
        fadeOut(channelBanner)
        bannerHideRunnable?.let { handler.removeCallbacks(it) }
        bannerHideRunnable = null
    }

    /**
     * Clean up resources - call in Activity.onDestroy
     */
    fun release() {
        bannerHideRunnable?.let { handler.removeCallbacks(it) }
        bannerHideRunnable = null

        // Cancel all animations
        loadingOverlay.animate().cancel()
        pauseOverlay.animate().cancel()
        errorOverlay.animate().cancel()
        channelBanner.animate().cancel()
    }

    private fun fadeIn(view: View, targetAlpha: Float = 1f) {
        if (view.visibility == View.VISIBLE && view.alpha == targetAlpha) return

        view.visibility = View.VISIBLE
        view.animate()
            .alpha(targetAlpha)
            .setDuration(FADE_DURATION)
            .start()
    }

    private fun fadeOut(view: View) {
        if (view.visibility == View.GONE) return

        view.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
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

        return if (isRetrying && retryAttempt < maxRetries) {
            "$errorDescription\nRetrying... ($retryAttempt/$maxRetries)"
        } else if (retryAttempt >= maxRetries) {
            "$errorDescription\nUnable to connect. Please check your internet connection."
        } else {
            errorDescription
        }
    }
}
