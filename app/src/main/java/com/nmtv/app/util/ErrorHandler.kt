package com.nmtv.app.util

import android.content.Context
import androidx.media3.common.PlaybackException
import com.nmtv.app.R
import com.nmtv.app.ui.overlay.PlaybackErrorType

/**
 * Sealed class hierarchy for playback errors.
 * Provides structured error handling with user-friendly messages.
 */
sealed class PlaybackError(
    open val type: PlaybackErrorType,
    open val message: String,
    open val userMessage: String,
    open val isRetryable: Boolean,
    open val actionHint: String?
) {
    /**
     * Network connectivity issues
     */
    data class NetworkError(
        override val message: String,
        val isTimeout: Boolean = false
    ) : PlaybackError(
        type = if (isTimeout) PlaybackErrorType.TIMEOUT else PlaybackErrorType.NETWORK,
        message = message,
        userMessage = if (isTimeout) "Connection timed out" else "Network connection failed",
        isRetryable = true,
        actionHint = "Check your internet connection"
    )

    /**
     * Stream not available or invalid
     */
    data class StreamError(
        override val message: String
    ) : PlaybackError(
        type = PlaybackErrorType.STREAM,
        message = message,
        userMessage = "Stream is currently unavailable",
        isRetryable = true,
        actionHint = "Try again later"
    )

    /**
     * Codec or format not supported
     */
    data class CodecError(
        override val message: String
    ) : PlaybackError(
        type = PlaybackErrorType.CODEC,
        message = message,
        userMessage = "Video format not supported",
        isRetryable = false,
        actionHint = "This content cannot be played on this device"
    )

    /**
     * Authentication or DRM issues
     */
    data class AuthError(
        override val message: String
    ) : PlaybackError(
        type = PlaybackErrorType.AUTH,
        message = message,
        userMessage = "Authentication required",
        isRetryable = false,
        actionHint = "Please sign in to view this content"
    )

    /**
     * Unknown or unhandled errors
     */
    data class UnknownError(
        override val message: String
    ) : PlaybackError(
        type = PlaybackErrorType.UNKNOWN,
        message = message,
        userMessage = "Something went wrong",
        isRetryable = true,
        actionHint = "Please try again"
    )
}

/**
 * Handles error mapping and user message generation.
 */
object ErrorHandler {

    /**
     * Map an ExoPlayer PlaybackException to our PlaybackError type.
     */
    fun mapException(exception: PlaybackException): PlaybackError {
        return when (exception.errorCode) {
            // Network errors
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                PlaybackError.NetworkError(
                    message = exception.message ?: "Network connection failed"
                )

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT ->
                PlaybackError.NetworkError(
                    message = exception.message ?: "Connection timeout",
                    isTimeout = true
                )

            // Stream errors
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
                PlaybackError.StreamError(
                    message = exception.message ?: "Stream not available"
                )

            // Codec errors
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ->
                PlaybackError.CodecError(
                    message = exception.message ?: "Codec error"
                )

            // Auth/DRM errors
            PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
            PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED ->
                PlaybackError.AuthError(
                    message = exception.message ?: "DRM/Authentication error"
                )

            // Unknown
            else -> PlaybackError.UnknownError(
                message = exception.message ?: "Unknown playback error"
            )
        }
    }

    /**
     * Get a user-friendly display message for an error.
     *
     * @param context Context for string resources
     * @param error The PlaybackError
     * @param retryAttempt Current retry attempt (0 = no retry yet)
     * @param maxRetries Maximum number of retries
     * @return User-friendly error message
     */
    fun getDisplayMessage(
        context: Context,
        error: PlaybackError,
        retryAttempt: Int = 0,
        maxRetries: Int = 5
    ): String {
        val baseMessage = error.userMessage

        return when {
            retryAttempt in 1 until maxRetries && error.isRetryable -> {
                "$baseMessage\nRetrying... ($retryAttempt/$maxRetries)"
            }
            retryAttempt >= maxRetries -> {
                context.getString(R.string.max_retries_exceeded)
            }
            error.actionHint != null -> {
                "$baseMessage\n${error.actionHint}"
            }
            else -> baseMessage
        }
    }

    /**
     * Determine if we should retry based on error type.
     */
    fun shouldRetry(error: PlaybackError, currentAttempt: Int, maxRetries: Int): Boolean {
        return error.isRetryable && currentAttempt < maxRetries
    }

    /**
     * Get the appropriate icon resource for an error type.
     */
    fun getErrorIconResource(errorType: PlaybackErrorType): Int {
        return when (errorType) {
            PlaybackErrorType.NETWORK,
            PlaybackErrorType.TIMEOUT -> android.R.drawable.stat_notify_sync_noanim
            PlaybackErrorType.STREAM -> android.R.drawable.ic_menu_close_clear_cancel
            PlaybackErrorType.CODEC -> android.R.drawable.ic_media_pause
            PlaybackErrorType.AUTH -> android.R.drawable.ic_lock_lock
            PlaybackErrorType.UNKNOWN -> android.R.drawable.stat_notify_error
        }
    }
}
