package com.nmtv.app.util

import androidx.media3.common.PlaybackException
import com.nmtv.app.ui.overlay.PlaybackErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ErrorHandler
 */
class ErrorHandlerTest {

    @Test
    fun `mapException maps network error correctly`() {
        val exception = PlaybackException(
            "Network error",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        val error = ErrorHandler.mapException(exception)

        assertTrue(error is PlaybackError.NetworkError)
        assertEquals(PlaybackErrorType.NETWORK, error.type)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `mapException maps timeout error correctly`() {
        val exception = PlaybackException(
            "Timeout",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )

        val error = ErrorHandler.mapException(exception)

        assertTrue(error is PlaybackError.NetworkError)
        assertEquals(PlaybackErrorType.TIMEOUT, error.type)
        assertTrue((error as PlaybackError.NetworkError).isTimeout)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `mapException maps stream error correctly`() {
        val exception = PlaybackException(
            "Not found",
            null,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )

        val error = ErrorHandler.mapException(exception)

        assertTrue(error is PlaybackError.StreamError)
        assertEquals(PlaybackErrorType.STREAM, error.type)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `mapException maps codec error correctly`() {
        val exception = PlaybackException(
            "Decoder error",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        )

        val error = ErrorHandler.mapException(exception)

        assertTrue(error is PlaybackError.CodecError)
        assertEquals(PlaybackErrorType.CODEC, error.type)
        assertFalse(error.isRetryable) // Codec errors are not retryable
    }

    @Test
    fun `mapException maps auth error correctly`() {
        val exception = PlaybackException(
            "DRM error",
            null,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED
        )

        val error = ErrorHandler.mapException(exception)

        assertTrue(error is PlaybackError.AuthError)
        assertEquals(PlaybackErrorType.AUTH, error.type)
        assertFalse(error.isRetryable) // Auth errors are not retryable
    }

    @Test
    fun `mapException maps unknown error correctly`() {
        val exception = PlaybackException(
            "Unknown",
            null,
            PlaybackException.ERROR_CODE_UNSPECIFIED
        )

        val error = ErrorHandler.mapException(exception)

        assertTrue(error is PlaybackError.UnknownError)
        assertEquals(PlaybackErrorType.UNKNOWN, error.type)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `shouldRetry returns true for retryable error under max`() {
        val error = PlaybackError.NetworkError("test")

        assertTrue(ErrorHandler.shouldRetry(error, currentAttempt = 1, maxRetries = 5))
        assertTrue(ErrorHandler.shouldRetry(error, currentAttempt = 4, maxRetries = 5))
    }

    @Test
    fun `shouldRetry returns false when max retries reached`() {
        val error = PlaybackError.NetworkError("test")

        assertFalse(ErrorHandler.shouldRetry(error, currentAttempt = 5, maxRetries = 5))
        assertFalse(ErrorHandler.shouldRetry(error, currentAttempt = 6, maxRetries = 5))
    }

    @Test
    fun `shouldRetry returns false for non-retryable error`() {
        val error = PlaybackError.CodecError("test")

        assertFalse(ErrorHandler.shouldRetry(error, currentAttempt = 1, maxRetries = 5))
    }

    @Test
    fun `NetworkError has correct properties`() {
        val error = PlaybackError.NetworkError("Connection failed")

        assertEquals(PlaybackErrorType.NETWORK, error.type)
        assertEquals("Network connection failed", error.userMessage)
        assertEquals("Check your internet connection", error.actionHint)
        assertTrue(error.isRetryable)
        assertFalse(error.isTimeout)
    }

    @Test
    fun `NetworkError with timeout has correct properties`() {
        val error = PlaybackError.NetworkError("Timeout", isTimeout = true)

        assertEquals(PlaybackErrorType.TIMEOUT, error.type)
        assertEquals("Connection timed out", error.userMessage)
        assertTrue(error.isRetryable)
        assertTrue(error.isTimeout)
    }

    @Test
    fun `StreamError has correct properties`() {
        val error = PlaybackError.StreamError("Not available")

        assertEquals(PlaybackErrorType.STREAM, error.type)
        assertEquals("Stream is currently unavailable", error.userMessage)
        assertEquals("Try again later", error.actionHint)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `CodecError has correct properties`() {
        val error = PlaybackError.CodecError("Unsupported format")

        assertEquals(PlaybackErrorType.CODEC, error.type)
        assertEquals("Video format not supported", error.userMessage)
        assertFalse(error.isRetryable)
    }

    @Test
    fun `AuthError has correct properties`() {
        val error = PlaybackError.AuthError("DRM failed")

        assertEquals(PlaybackErrorType.AUTH, error.type)
        assertEquals("Authentication required", error.userMessage)
        assertFalse(error.isRetryable)
    }
}
