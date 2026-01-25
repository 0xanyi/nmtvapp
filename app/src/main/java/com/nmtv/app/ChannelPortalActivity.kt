package com.nmtv.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import coil.load
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository
import com.nmtv.app.util.ImageManager

/**
 * Channel selection portal that displays available channels as cards.
 * Uses Apple TV-like effects (blur, reflections) for a premium experience.
 *
 * For apps with many channels (10+), consider using LeanbackPortalActivity instead
 * which provides proper Leanback BrowseFragment-based scrolling.
 */
class ChannelPortalActivity : FragmentActivity() {

    private lateinit var channelRepository: ChannelRepository
    private lateinit var channels: List<Channel>
    private lateinit var imageManager: ImageManager

    private lateinit var cardNmtv: View
    private lateinit var cardClassics: View
    private lateinit var cardNmtvPreview: ImageView
    private lateinit var cardClassicsPreview: ImageView
    private lateinit var cardNmtvTitle: TextView
    private lateinit var cardClassicsTitle: TextView
    private lateinit var cardNmtvDesc: TextView
    private lateinit var cardClassicsDesc: TextView

    private lateinit var rootLayout: View
    private lateinit var blurBackground: ImageView
    private lateinit var cardNmtvReflection: ImageView
    private lateinit var cardClassicsReflection: ImageView

    private var selectedChannelIndex = 0
    private val channelPreviews = mapOf(
        "nmtv_uk" to R.drawable.nmtv_live,
        "nmtv_classics" to R.drawable.nmtv_classics
    )

    // Cache for blurred backgrounds by resource ID
    private val blurredBackgroundCache = mutableMapOf<Int, Bitmap>()

    companion object {
        private const val TAG = "ChannelPortalActivity"
        private const val FOCUS_SCALE = 1.08f
        private const val FOCUS_DURATION = 300L
        private const val ELEVATION_UNFOCUSED = 12f
        private const val ELEVATION_FOCUSED = 24f
        private const val REFLECTION_ALPHA = 0.4f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        setContentView(R.layout.activity_channel_portal)

        // Initialize image manager
        imageManager = ImageManager.getInstance(this)

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
        
        // Set preview images using Coil for efficient loading
        channelPreviews["nmtv_uk"]?.let {
            cardNmtvPreview.load(it) {
                crossfade(true)
            }
        }
        channelPreviews["nmtv_classics"]?.let {
            cardClassicsPreview.load(it) {
                crossfade(true)
            }
        }

        // Initialize new views for Apple TV effects
        rootLayout = findViewById(R.id.rootLayout)
        blurBackground = findViewById(R.id.blurBackground)
        cardNmtvReflection = findViewById(R.id.cardNmtvReflection)
        cardClassicsReflection = findViewById(R.id.cardClassicsReflection)
    }
    
    private fun setupFocusHandling() {
        // Set focus change listeners for visual feedback
        cardNmtv.setOnFocusChangeListener { _, hasFocus ->
            updateCardFocus(cardNmtv, hasFocus)
            if (hasFocus) {
                channelPreviews["nmtv_uk"]?.let { resId ->
                    updateBlurredBackground(cardNmtvPreview, resId)
                }
                showReflection(cardNmtv, cardNmtvReflection, cardNmtvPreview, true)
            } else {
                showReflection(cardNmtv, cardNmtvReflection, cardNmtvPreview, false)
            }
        }

        cardClassics.setOnFocusChangeListener { _, hasFocus ->
            updateCardFocus(cardClassics, hasFocus)
            if (hasFocus) {
                channelPreviews["nmtv_classics"]?.let { resId ->
                    updateBlurredBackground(cardClassicsPreview, resId)
                }
                showReflection(cardClassics, cardClassicsReflection, cardClassicsPreview, true)
            } else {
                showReflection(cardClassics, cardClassicsReflection, cardClassicsPreview, false)
            }
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
        val interpolator = OvershootInterpolator(1.5f)
        
        if (hasFocus) {
            card.animate()
                .scaleX(FOCUS_SCALE)
                .scaleY(FOCUS_SCALE)
                .setDuration(FOCUS_DURATION)
                .setInterpolator(interpolator)
                .start()
            
            ObjectAnimator.ofFloat(card, "elevation", ELEVATION_UNFOCUSED, ELEVATION_FOCUSED).apply {
                duration = FOCUS_DURATION
                start()
            }
            
            selectedChannelIndex = if (card == cardNmtv) 0 else 1
        } else {
            card.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(FOCUS_DURATION)
                .setInterpolator(interpolator)
                .start()
            
            ObjectAnimator.ofFloat(card, "elevation", ELEVATION_FOCUSED, ELEVATION_UNFOCUSED).apply {
                duration = FOCUS_DURATION
                start()
            }
        }
    }
    
    private fun updateBlurredBackground(previewImage: ImageView, resourceId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check cache first
            val cachedBitmap = blurredBackgroundCache[resourceId]
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                blurBackground.setImageBitmap(cachedBitmap)
                blurBackground.setRenderEffect(
                    RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
                )
                return
            }

            // Get bitmap from preview and cache it
            val drawable = previewImage.drawable ?: return
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return

            // Create a copy for the cache (original may be recycled)
            val bitmapCopy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
            blurredBackgroundCache[resourceId] = bitmapCopy

            blurBackground.setImageBitmap(bitmapCopy)
            blurBackground.setRenderEffect(
                RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
            )
        }
    }
    
    private fun showReflection(card: View, reflection: ImageView, preview: ImageView, show: Boolean) {
        val targetAlpha = if (show) REFLECTION_ALPHA else 0f
        reflection.animate()
            .alpha(targetAlpha)
            .setDuration(FOCUS_DURATION)
            .start()
        if (show) {
            reflection.setImageDrawable(preview.drawable)
            reflection.visibility = View.VISIBLE
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

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all ongoing animations to prevent memory leaks
        cardNmtv.animate().cancel()
        cardNmtv.clearAnimation()
        cardClassics.animate().cancel()
        cardClassics.clearAnimation()
        cardNmtvReflection.animate().cancel()
        cardNmtvReflection.clearAnimation()
        cardClassicsReflection.animate().cancel()
        cardClassicsReflection.clearAnimation()

        // Clean up cached bitmaps
        blurredBackgroundCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        blurredBackgroundCache.clear()
    }
}
