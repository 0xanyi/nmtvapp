package com.nmtv.app.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.nmtv.app.R

/**
 * Leanback-based channel portal that uses Fragment architecture.
 * This activity hosts BrowseChannelsFragment for scalable channel browsing.
 *
 * Use this when you have many channels. For 2-3 channels, the original
 * ChannelPortalActivity provides a more polished Apple TV-like experience.
 */
class LeanbackPortalActivity : FragmentActivity() {

    companion object {
        private const val TAG = "LeanbackPortalActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leanback_portal)

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

        // Add the browse fragment if this is first creation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.browse_fragment_container, BrowseChannelsFragment())
                .commit()
        }
    }
}
