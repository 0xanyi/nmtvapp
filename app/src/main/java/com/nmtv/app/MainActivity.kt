package com.nmtv.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository
import com.nmtv.app.player.StreamPlayer
import com.nmtv.app.player.StreamPlayerListener
import com.nmtv.app.ui.overlay.PlayerOverlayManager

/**
 * Main activity that displays fullscreen HLS video playback.
 * Launches directly into video playback with minimal UI.
 */
class MainActivity : android.app.Activity(), StreamPlayerListener {

    private lateinit var playerView: PlayerView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var pauseOverlay: FrameLayout
    private lateinit var errorOverlay: FrameLayout
    private lateinit var errorMessage: TextView
    private lateinit var errorProgressBar: ProgressBar

    private lateinit var streamPlayer: StreamPlayer
    private lateinit var channelRepository: ChannelRepository
    private lateinit var overlayManager: PlayerOverlayManager
    private var currentChannel: Channel? = null

    // Channel banner views
    private lateinit var channelBanner: FrameLayout
    private lateinit var channelBannerTitle: TextView
    private lateinit var channelBannerInfo: TextView

    companion object {
        private const val TAG = "MainActivity"
        private const val MAX_RETRIES = 5
    }

    // Track retry attempts for UI display
    private var currentRetryAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(TAG, "Content view set")

            // Set fullscreen flags after decor view exists to avoid NPE on TV emulator
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.decorView.windowInsetsController?.hide(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }

            // Initialize views
            playerView = findViewById(R.id.playerView)
            loadingOverlay = findViewById(R.id.loadingOverlay)
            pauseOverlay = findViewById(R.id.pauseOverlay)
            errorOverlay = findViewById(R.id.errorOverlay)
            errorMessage = findViewById(R.id.errorMessage)
            errorProgressBar = findViewById(R.id.errorProgressBar)
            
            // Initialize channel banner
            channelBanner = findViewById(R.id.channelBanner)
            channelBannerTitle = findViewById(R.id.channelBannerTitle)
            channelBannerInfo = findViewById(R.id.channelBannerInfo)

            // Initialize overlay manager with state machine
            overlayManager = PlayerOverlayManager(
                loadingOverlay = loadingOverlay,
                pauseOverlay = pauseOverlay,
                errorOverlay = errorOverlay,
                errorMessage = errorMessage,
                errorProgressBar = errorProgressBar,
                channelBanner = channelBanner,
                channelBannerTitle = channelBannerTitle,
                channelBannerInfo = channelBannerInfo
            )

            Log.d(TAG, "Views initialized")

            // Initialize repository
            channelRepository = LocalChannelRepository()
            Log.d(TAG, "Channel repository initialized")

            // Initialize player
            streamPlayer = StreamPlayer(this, playerView)
            streamPlayer.setListener(this)
            streamPlayer.initialize()
            Log.d(TAG, "Stream player initialized")

            // Check if channel was selected from portal
            val selectedChannelId = intent.getStringExtra("channel_id")
            currentChannel = if (selectedChannelId != null) {
                channelRepository.getChannelById(selectedChannelId)
            } else {
                channelRepository.getDefaultChannel()
            }
            
            Log.d(TAG, "Selected channel: ${currentChannel?.name}")
            
            currentChannel?.let { channel ->
                Log.d(TAG, "Starting playback: ${channel.name}")  // Removed URL from log
                streamPlayer.play(channel.streamUrl)
                // Show channel banner briefly when starting
                overlayManager.showChannelBanner(channel.name)
            } ?: run {
                Log.e(TAG, "ERROR: No channel found!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "CRASH in onCreate: ${e.message}", e)
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        // Smart resume: only restart if player is in an error or idle state
        // Don't interrupt if already playing or buffering
        currentChannel?.let { channel ->
            val playerState = streamPlayer.playbackState
            when (playerState) {
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    // Player stopped or ended, restart stream
                    Log.d(TAG, "Restarting stream on resume (state: $playerState)")
                    streamPlayer.play(channel.streamUrl)
                }
                Player.STATE_READY -> {
                    // Player is ready but paused, just resume playback
                    if (!streamPlayer.isPlaying()) {
                        Log.d(TAG, "Resuming paused stream")
                        streamPlayer.resume()
                    } else {
                        Log.d(TAG, "Stream already playing, no action needed")
                    }
                }
                Player.STATE_BUFFERING -> {
                    // Already buffering, don't interrupt
                    Log.d(TAG, "Stream buffering, no action needed")
                }
                else -> {
                    // Unknown state, do nothing
                    Log.d(TAG, "Unknown player state: $playerState")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        streamPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release overlay manager resources
        overlayManager.release()
        streamPlayer.release()
    }

    // StreamPlayerListener implementation

    override fun onBuffering() {
        runOnUiThread {
            overlayManager.showLoading()
            Log.d(TAG, "Buffering...")
        }
    }

    override fun onPlaying() {
        runOnUiThread {
            overlayManager.hide()
            currentRetryAttempt = 0 // Reset retry counter on successful playback
            Log.d(TAG, "Playing")
        }
    }

    override fun onPaused() {
        runOnUiThread {
            overlayManager.showPaused()
            Log.d(TAG, "Paused")
        }
    }

    override fun onError(error: PlaybackException) {
        runOnUiThread {
            currentRetryAttempt++
            val isRetrying = currentRetryAttempt < MAX_RETRIES
            overlayManager.showError(error, currentRetryAttempt, MAX_RETRIES, isRetrying)
            Log.e(TAG, "Playback error (attempt $currentRetryAttempt/$MAX_RETRIES)", error)
        }
    }

    override fun onEnded() {
        runOnUiThread {
            Log.d(TAG, "Playback ended")
            // For live streams, this shouldn't normally happen
            // Attempt to restart
            currentChannel?.let { channel ->
                streamPlayer.play(channel.streamUrl)
            }
        }
    }

    // Remote control handling

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Reserved for future channel up
                switchToNextChannel()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Reserved for future channel down
                switchToPreviousChannel()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                // Return to channel portal instead of exiting
                val intent = Intent(this, ChannelPortalActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun togglePlayPause() {
        if (streamPlayer.isPlaying()) {
            streamPlayer.pause()
        } else {
            streamPlayer.resume()
        }
    }

    private fun switchToNextChannel() {
        currentChannel?.let { channel ->
            channelRepository.getNextChannel(channel.id)?.let { nextChannel ->
                Log.d(TAG, "Switching to next channel: ${nextChannel.name}")
                currentChannel = nextChannel
                currentRetryAttempt = 0 // Reset retry counter on channel switch
                streamPlayer.play(nextChannel.streamUrl)
                overlayManager.showChannelBanner(nextChannel.name)
            }
        }
    }

    private fun switchToPreviousChannel() {
        currentChannel?.let { channel ->
            channelRepository.getPreviousChannel(channel.id)?.let { prevChannel ->
                Log.d(TAG, "Switching to previous channel: ${prevChannel.name}")
                currentChannel = prevChannel
                currentRetryAttempt = 0 // Reset retry counter on channel switch
                streamPlayer.play(prevChannel.streamUrl)
                overlayManager.showChannelBanner(prevChannel.name)
            }
        }
    }
}