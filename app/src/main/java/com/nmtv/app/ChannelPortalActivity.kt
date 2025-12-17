package com.nmtv.app

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository

/**
 * Channel selection portal that displays available channels as cards.
 * Users can navigate and select channels before entering full playback.
 */
class ChannelPortalActivity : android.app.Activity() {

    private lateinit var channelRepository: ChannelRepository
    private lateinit var channels: List<Channel>
    
    private lateinit var cardNmtv: View
    private lateinit var cardClassics: View
    private lateinit var cardNmtvPreview: ImageView
    private lateinit var cardClassicsPreview: ImageView
    private lateinit var cardNmtvTitle: TextView
    private lateinit var cardClassicsTitle: TextView
    private lateinit var cardNmtvDesc: TextView
    private lateinit var cardClassicsDesc: TextView
    
    private var selectedChannelIndex = 0
    private val channelPreviews = mapOf(
        "nmtv_uk" to R.drawable.preview_nmtv,
        "nmtv_classics" to R.drawable.preview_classics
    )

    companion object {
        private const val TAG = "ChannelPortalActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        
        setContentView(R.layout.activity_channel_portal)
        
        // Set fullscreen flags
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        
        // Initialize channel repository
        channelRepository = LocalChannelRepository()
        channels = channelRepository.getAllChannels()
        
        // Initialize views
        initializeViews()
        setupFocusHandling()
        
        // Set initial focus
        cardNmtv.requestFocus()
    }
    
    private fun initializeViews() {
        cardNmtv = findViewById(R.id.cardNmtv)
        cardClassics = findViewById(R.id.cardClassics)
        cardNmtvPreview = findViewById(R.id.cardNmtvPreview)
        cardClassicsPreview = findViewById(R.id.cardClassicsPreview)
        cardNmtvTitle = findViewById(R.id.cardNmtvTitle)
        cardClassicsTitle = findViewById(R.id.cardClassicsTitle)
        cardNmtvDesc = findViewById(R.id.cardNmtvDesc)
        cardClassicsDesc = findViewById(R.id.cardClassicsDesc)
        
        // Set channel information
        val nmtvChannel = channels.find { it.id == "nmtv_uk" }
        val classicsChannel = channels.find { it.id == "nmtv_classics" }
        
        cardNmtvTitle.text = nmtvChannel?.name ?: "NMTV UK"
        cardClassicsTitle.text = classicsChannel?.name ?: "NMTV Classics"
        cardNmtvDesc.text = "Live streaming from the UK with the latest content"
        cardClassicsDesc.text = "Classic shows and timeless entertainment"
        
        // Set preview images
        channelPreviews["nmtv_uk"]?.let { cardNmtvPreview.setImageResource(it) }
        channelPreviews["nmtv_classics"]?.let { cardClassicsPreview.setImageResource(it) }
    }
    
    private fun setupFocusHandling() {
        // Set focus change listeners for visual feedback
        cardNmtv.setOnFocusChangeListener { _, hasFocus ->
            updateCardFocus(cardNmtv, hasFocus)
        }
        
        cardClassics.setOnFocusChangeListener { _, hasFocus ->
            updateCardFocus(cardClassics, hasFocus)
        }
        
        // Set click listeners
        cardNmtv.setOnClickListener {
            launchChannel("nmtv_uk")
        }
        
        cardClassics.setOnClickListener {
            launchChannel("nmtv_classics")
        }
    }
    
    private fun updateCardFocus(card: View, hasFocus: Boolean) {
        if (hasFocus) {
            // Scale up and add glow effect
            card.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start()
            
            // Add glow effect on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                card.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
            }
            
            // Update selection
            selectedChannelIndex = if (card == cardNmtv) 0 else 1
        } else {
            // Scale back down
            card.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start()
            
            // Remove glow effect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                card.setRenderEffect(null)
            }
        }
    }
    
    private fun launchChannel(channelId: String) {
        Log.d(TAG, "Launching channel: $channelId")
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("channel_id", channelId)
        }
        startActivity(intent)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (selectedChannelIndex == 1) {
                    cardNmtv.requestFocus()
                    true
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (selectedChannelIndex == 0) {
                    cardClassics.requestFocus()
                    true
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                val focusedView = currentFocus
                when (focusedView) {
                    cardNmtv -> launchChannel("nmtv_uk")
                    cardClassics -> launchChannel("nmtv_classics")
                }
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reset focus to first card when returning
        cardNmtv.requestFocus()
    }
}
