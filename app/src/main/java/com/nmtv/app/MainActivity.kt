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
import androidx.media3.ui.PlayerView
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository
import com.nmtv.app.player.StreamPlayer
import com.nmtv.app.player.StreamPlayerListener

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
    private var currentChannel: Channel? = null
    
    // Channel banner views
    private lateinit var channelBanner: FrameLayout
    private lateinit var channelBannerTitle: TextView
    private lateinit var channelBannerInfo: TextView
    private var bannerHideRunnable: Runnable? = null

    companion object {
        private const val TAG = "MainActivity"
    }

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
                Log.d(TAG, "Starting playback: ${channel.name} - URL: ${channel.streamUrl}")
                streamPlayer.play(channel.streamUrl)
                // Show channel banner briefly when starting
                showChannelBanner(channel)
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
        // Always restart stream on resume (as per architecture decision)
        currentChannel?.let { channel ->
            streamPlayer.play(channel.streamUrl)
        }
    }

    override fun onPause() {
        super.onPause()
        streamPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerHideRunnable?.let {
            window.decorView.removeCallbacks(it)
        }
        streamPlayer.release()
    }

    // StreamPlayerListener implementation

    override fun onBuffering() {
        runOnUiThread {
            loadingOverlay.visibility = View.VISIBLE
            pauseOverlay.visibility = View.GONE
            errorOverlay.visibility = View.GONE
            Log.d(TAG, "Buffering...")
        }
    }

    override fun onPlaying() {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
            pauseOverlay.visibility = View.GONE
            errorOverlay.visibility = View.GONE
            Log.d(TAG, "Playing")
        }
    }

    override fun onPaused() {
        runOnUiThread {
            // Only show pause overlay if we're not buffering or showing error
            if (loadingOverlay.visibility != View.VISIBLE && errorOverlay.visibility != View.VISIBLE) {
                loadingOverlay.visibility = View.GONE
                pauseOverlay.visibility = View.VISIBLE
                errorOverlay.visibility = View.GONE
                Log.d(TAG, "Paused - overlay shown")
            } else {
                Log.d(TAG, "Paused - overlay hidden due to other state")
            }
        }
    }

    override fun onError(error: PlaybackException) {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
            pauseOverlay.visibility = View.GONE
            errorOverlay.visibility = View.VISIBLE
            errorProgressBar.visibility = View.VISIBLE
            errorMessage.text = getString(R.string.retrying_connection)
            Log.e(TAG, "Playback error", error)
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
                streamPlayer.play(nextChannel.streamUrl)
                showChannelBanner(nextChannel)
            }
        }
    }

    private fun switchToPreviousChannel() {
        currentChannel?.let { channel ->
            channelRepository.getPreviousChannel(channel.id)?.let { prevChannel ->
                Log.d(TAG, "Switching to previous channel: ${prevChannel.name}")
                currentChannel = prevChannel
                streamPlayer.play(prevChannel.streamUrl)
                showChannelBanner(prevChannel)
            }
        }
    }
    
    private fun showChannelBanner(channel: Channel) {
        runOnUiThread {
            // Cancel any pending hide
            bannerHideRunnable?.let {
                window.decorView.removeCallbacks(it)
            }
            
            // Update banner content
            channelBannerTitle.text = channel.name
            channelBannerInfo.text = "Now Playing: Live Stream"
            
            // Show banner with animation
            channelBanner.visibility = View.VISIBLE
            channelBanner.alpha = 0f
            channelBanner.animate()
                .alpha(0.9f)
                .setDuration(300)
                .start()
            
            // Schedule auto-hide after 3 seconds
            bannerHideRunnable = Runnable {
                hideChannelBanner()
            }
            window.decorView.postDelayed(bannerHideRunnable!!, 3000)
        }
    }
    
    private fun hideChannelBanner() {
        runOnUiThread {
            channelBanner.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    channelBanner.visibility = View.GONE
                }
                .start()
        }
    }
}