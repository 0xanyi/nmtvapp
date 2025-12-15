package com.nmtv.app

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
class MainActivity : AppCompatActivity(), StreamPlayerListener {

    private lateinit var playerView: PlayerView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var pauseOverlay: FrameLayout
    private lateinit var errorOverlay: FrameLayout
    private lateinit var errorMessage: TextView
    private lateinit var errorProgressBar: ProgressBar

    private lateinit var streamPlayer: StreamPlayer
    private lateinit var channelRepository: ChannelRepository
    private var currentChannel: Channel? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set fullscreen flags
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        setContentView(R.layout.activity_main)

        // Initialize views
        playerView = findViewById(R.id.playerView)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        errorOverlay = findViewById(R.id.errorOverlay)
        errorMessage = findViewById(R.id.errorMessage)
        errorProgressBar = findViewById(R.id.errorProgressBar)

        // Initialize repository
        channelRepository = LocalChannelRepository()

        // Initialize player
        streamPlayer = StreamPlayer(this, playerView)
        streamPlayer.setListener(this)
        streamPlayer.initialize()

        // Get default channel and start playback
        currentChannel = channelRepository.getDefaultChannel()
        currentChannel?.let { channel ->
            Log.d(TAG, "Starting playback: ${channel.name}")
            streamPlayer.play(channel.streamUrl)
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
            loadingOverlay.visibility = View.GONE
            pauseOverlay.visibility = View.VISIBLE
            errorOverlay.visibility = View.GONE
            Log.d(TAG, "Paused")
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
                // Exit app on back button
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
            }
        }
    }

    private fun switchToPreviousChannel() {
        currentChannel?.let { channel ->
            channelRepository.getPreviousChannel(channel.id)?.let { prevChannel ->
                Log.d(TAG, "Switching to previous channel: ${prevChannel.name}")
                currentChannel = prevChannel
                streamPlayer.play(prevChannel.streamUrl)
            }
        }
    }
}