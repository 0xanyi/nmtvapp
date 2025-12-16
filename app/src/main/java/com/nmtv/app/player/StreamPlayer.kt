package com.nmtv.app.player

import android.content.Context
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

    companion object {
        private const val TAG = "StreamPlayer"
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
                    
                    // Set up player event listener
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    Log.d(TAG, "State: Buffering")
                                    listener?.onBuffering()
                                }
                                Player.STATE_READY -> {
                                    Log.d(TAG, "State: Ready (playWhenReady=$playWhenReady)")
                                    if (playWhenReady) {
                                        listener?.onPlaying()
                                        retryAttempt = 0 // Reset retry counter on success
                                        retryManager.reset()
                                    } else {
                                        listener?.onPaused()
                                    }
                                }
                                Player.STATE_ENDED -> {
                                    Log.d(TAG, "State: Ended")
                                    listener?.onEnded()
                                }
                                Player.STATE_IDLE -> {
                                    Log.d(TAG, "State: Idle")
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.d(TAG, "onIsPlayingChanged: $isPlaying (state=${playbackState})")
                            if (!isPlaying && playbackState == Player.STATE_READY) {
                                listener?.onPaused()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "PLAYBACK ERROR: ${error.message} (errorCode=${error.errorCode})", error)
                            listener?.onError(error)
                            
                            // Attempt to reconnect with exponential backoff
                            if (retryAttempt < 5) {
                                Log.d(TAG, "Scheduling retry attempt ${retryAttempt + 1}/5")
                                retryManager.scheduleRetry(retryAttempt) {
                                    Log.d(TAG, "Executing retry attempt ${retryAttempt + 1}")
                                    retryAttempt++
                                    currentStreamUrl?.let { url ->
                                        play(url)
                                    }
                                }
                            } else {
                                Log.e(TAG, "Max retries exceeded, giving up")
                            }
                        }
                    })

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
     * Start playing an HLS stream.
     *
     * @param streamUrl The HLS stream URL (m3u8 playlist)
     */
    @OptIn(UnstableApi::class)
    fun play(streamUrl: String) {
        Log.d(TAG, "play() called with URL: $streamUrl")
        currentStreamUrl = streamUrl
        
        exoPlayer?.let { player ->
            try {
                Log.d(TAG, "Creating HLS media source...")
                // Create HLS media source
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                
                val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl))
                
                Log.d(TAG, "Media source created, preparing player...")

                // Prepare and play
                player.setMediaSource(mediaSource)
                player.prepare()
                player.playWhenReady = true

                Log.d(TAG, "Playing stream: $streamUrl")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR in play(): ${e.message}", e)
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
            playerView.player = null
            player.release()
            Log.d(TAG, "Player released")
        }
        exoPlayer = null
        currentStreamUrl = null
        retryAttempt = 0
    }
}