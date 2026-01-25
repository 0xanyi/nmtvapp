package com.nmtv.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView

/**
 * Wrapper class for ExoPlayer that handles HLS stream playback.
 * Encapsulates all player logic and provides a clean interface to the UI.
 *
 * @property context Application context
 * @property playerView The PlayerView to render video
 */
class StreamPlayer(
    private val context: Context,
    private val playerView: PlayerView
) {
    private var exoPlayer: ExoPlayer? = null
    private var listener: StreamPlayerListener? = null
    private val retryManager = RetryManager()
    private var currentStreamUrl: String? = null
    private var retryAttempt = 0
    private var playerListener: Player.Listener? = null

    // Synchronization lock for play() calls
    private val playLock = Any()

    companion object {
        private const val TAG = "StreamPlayer"

        // Network timeouts
        private const val CONNECT_TIMEOUT_MS = 15000  // 15 seconds
        private const val READ_TIMEOUT_MS = 30000     // 30 seconds
        private const val MAX_RETRIES = 5

        // User agent for stream requests
        private const val USER_AGENT = "NMTV-AndroidTV/1.0"

        // Whitelist of allowed stream domains for security
        private val ALLOWED_STREAM_DOMAINS = listOf(
            "wowza.com",
            "nmtv.tv"
        )

        // Error codes that should trigger retry (transient errors)
        private val RETRYABLE_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        )

        /**
         * Validates that a stream URL is safe to use.
         * Ensures HTTPS and whitelisted domains.
         */
        private fun isValidStreamUrl(url: String): Boolean {
            return try {
                val uri = Uri.parse(url)
                val scheme = uri.scheme
                val host = uri.host

                // Enforce HTTPS for security
                if (scheme != "https") {
                    Log.w(TAG, "URL validation failed: Non-HTTPS scheme")
                    return false
                }

                // Check against domain whitelist
                val isWhitelisted = ALLOWED_STREAM_DOMAINS.any { allowedDomain ->
                    host?.endsWith(allowedDomain) == true
                }

                if (!isWhitelisted) {
                    Log.w(TAG, "URL validation failed: Domain not whitelisted")
                }

                isWhitelisted
            } catch (e: Exception) {
                Log.e(TAG, "URL validation failed with exception", e)
                false
            }
        }

        /**
         * Check if an error is transient and should be retried.
         */
        private fun isRetryableError(error: PlaybackException): Boolean {
            return error.errorCode in RETRYABLE_ERROR_CODES
        }
    }

    /**
     * Initialize the ExoPlayer instance.
     */
    fun initialize() {
        Log.d(TAG, "initialize() called")
        release() // Release any existing player

        try {
            exoPlayer = ExoPlayer.Builder(context)
                .build()
                .apply {
                    Log.d(TAG, "ExoPlayer instance created")

                    // Set up player event listener with named class to avoid memory leak
                    playerListener = StreamPlayerEventListener(this@StreamPlayer)
                    addListener(playerListener!!)

                    // Attach player to view
                    playerView.player = this
                    Log.d(TAG, "Player attached to PlayerView")

                    // Configure player view
                    playerView.useController = false // Hide default controls
                    playerView.keepScreenOn = true
                    Log.d(TAG, "PlayerView configured")
                }

            Log.d(TAG, "Player initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR in initialize(): ${e.message}", e)
            throw e
        }
    }

    /**
     * Named listener class to avoid memory leaks from anonymous inner classes.
     * Uses weak reference to prevent retaining StreamPlayer after release.
     */
    private class StreamPlayerEventListener(
        streamPlayer: StreamPlayer
    ) : Player.Listener {
        private val weakStreamPlayer = java.lang.ref.WeakReference(streamPlayer)

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = weakStreamPlayer.get() ?: return
            val exoPlayer = player.exoPlayer ?: return

            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    Log.d(TAG, "State: Buffering")
                    player.listener?.onBuffering()
                }
                Player.STATE_READY -> {
                    Log.d(TAG, "State: Ready (playWhenReady=${exoPlayer.playWhenReady})")
                    if (exoPlayer.playWhenReady) {
                        player.listener?.onPlaying()
                        player.retryAttempt = 0 // Reset retry counter on success
                        player.retryManager.reset()
                    } else {
                        player.listener?.onPaused()
                    }
                }
                Player.STATE_ENDED -> {
                    Log.d(TAG, "State: Ended")
                    player.listener?.onEnded()
                }
                Player.STATE_IDLE -> {
                    Log.d(TAG, "State: Idle")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = weakStreamPlayer.get() ?: return
            val exoPlayer = player.exoPlayer ?: return

            Log.d(TAG, "onIsPlayingChanged: $isPlaying (state=${exoPlayer.playbackState})")
            if (isPlaying) {
                player.listener?.onPlaying()
            } else if (exoPlayer.playbackState == Player.STATE_READY) {
                player.listener?.onPaused()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val player = weakStreamPlayer.get() ?: return

            Log.e(TAG, "PLAYBACK ERROR: ${error.message} (errorCode=${error.errorCode})", error)
            player.listener?.onError(error)

            // Only retry on transient errors
            if (!isRetryableError(error)) {
                Log.e(TAG, "Non-retryable error, not scheduling retry")
                return
            }

            // Attempt to reconnect with exponential backoff
            if (player.retryAttempt < MAX_RETRIES) {
                Log.d(TAG, "Scheduling retry attempt ${player.retryAttempt + 1}/$MAX_RETRIES")
                player.retryManager.scheduleRetry(player.retryAttempt) {
                    Log.d(TAG, "Executing retry attempt ${player.retryAttempt + 1}")
                    player.retryAttempt++
                    player.currentStreamUrl?.let { url ->
                        player.play(url)
                    }
                }
            } else {
                Log.e(TAG, "Max retries exceeded, giving up")
            }
        }
    }

    /**
     * Start playing an HLS stream.
     *
     * @param streamUrl The HLS stream URL (m3u8 playlist)
     */
    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    fun play(streamUrl: String) {
        // Synchronize to prevent concurrent play() calls
        synchronized(playLock) {
            // Validate URL before use - security check
            if (!isValidStreamUrl(streamUrl)) {
                Log.e(TAG, "SECURITY: Invalid or unsafe stream URL rejected")
                listener?.onError(
                    PlaybackException(
                        "Invalid stream URL",
                        null,
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                    )
                )
                return
            }

            Log.d(TAG, "play() called")  // Removed URL from log for security
            currentStreamUrl = streamUrl

            exoPlayer?.let { player ->
                try {
                    Log.d(TAG, "Creating HLS media source...")
                    // Create HLS media source with timeouts
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                        .setReadTimeoutMs(READ_TIMEOUT_MS)
                        .setAllowCrossProtocolRedirects(true)
                        .setUserAgent(USER_AGENT)

                    val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(streamUrl))

                    Log.d(TAG, "Media source created, preparing player...")

                    // Prepare and play
                    player.setMediaSource(mediaSource)
                    player.prepare()
                    player.playWhenReady = true

                    Log.d(TAG, "Stream playback started")  // Removed URL from log
                } catch (e: Exception) {
                    Log.e(TAG, "ERROR in play()", e)  // Removed message to avoid URL leak
                    listener?.onError(
                        PlaybackException(
                            e.message ?: "Unexpected playback error",
                            e,
                            PlaybackException.ERROR_CODE_UNSPECIFIED
                        )
                    )
                }
            } ?: run {
                Log.e(TAG, "CRITICAL ERROR: Cannot play - Player not initialized!")
            }
        }
    }

    /**
     * Pause playback.
     */
    fun pause() {
        exoPlayer?.playWhenReady = false
        Log.d(TAG, "Playback paused")
    }

    /**
     * Resume playback.
     */
    fun resume() {
        exoPlayer?.playWhenReady = true
        Log.d(TAG, "Playback resumed")
    }

    /**
     * Check if player is currently playing.
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    /**
     * Get the current playback state.
     */
    val playbackState: Int
        get() = exoPlayer?.playbackState ?: Player.STATE_IDLE

    /**
     * Set the listener for player state changes.
     */
    fun setListener(listener: StreamPlayerListener) {
        this.listener = listener
    }

    /**
     * Release all player resources.
     * Must be called when the player is no longer needed.
     */
    fun release() {
        retryManager.cancel()
        exoPlayer?.let { player ->
            // Remove listener before releasing to prevent callbacks
            playerListener?.let { player.removeListener(it) }
            playerListener = null

            playerView.player = null
            player.release()
            Log.d(TAG, "Player released")
        }
        exoPlayer = null
        currentStreamUrl = null
        retryAttempt = 0
        listener = null
    }
}