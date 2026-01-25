package com.nmtv.app

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for MainActivity overlay states.
 * Tests overlay visibility and state transitions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    private fun launchWithChannel(channelId: String? = null): ActivityScenario<MainActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        channelId?.let { intent.putExtra("channel_id", it) }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun playerViewIsDisplayed() {
        val scenario = launchWithChannel("nmtv_uk")

        onView(withId(R.id.playerView))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun loadingOverlayIsVisibleOnStart() {
        val scenario = launchWithChannel("nmtv_uk")

        // Loading overlay should be visible initially
        onView(withId(R.id.loadingOverlay))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun overlayViewsExist() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val loadingOverlay = activity.findViewById<View>(R.id.loadingOverlay)
            val pauseOverlay = activity.findViewById<View>(R.id.pauseOverlay)
            val errorOverlay = activity.findViewById<View>(R.id.errorOverlay)
            val channelBanner = activity.findViewById<View>(R.id.channelBanner)

            assert(loadingOverlay != null) { "Loading overlay should exist" }
            assert(pauseOverlay != null) { "Pause overlay should exist" }
            assert(errorOverlay != null) { "Error overlay should exist" }
            assert(channelBanner != null) { "Channel banner should exist" }
        }

        scenario.close()
    }

    @Test
    fun errorOverlayContainsErrorMessage() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val errorMessage = activity.findViewById<android.widget.TextView>(R.id.errorMessage)
            val errorProgressBar = activity.findViewById<android.widget.ProgressBar>(R.id.errorProgressBar)

            assert(errorMessage != null) { "Error message TextView should exist" }
            assert(errorProgressBar != null) { "Error progress bar should exist" }
        }

        scenario.close()
    }

    @Test
    fun channelBannerContainsRequiredViews() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val bannerTitle = activity.findViewById<android.widget.TextView>(R.id.channelBannerTitle)
            val bannerInfo = activity.findViewById<android.widget.TextView>(R.id.channelBannerInfo)

            assert(bannerTitle != null) { "Channel banner title should exist" }
            assert(bannerInfo != null) { "Channel banner info should exist" }
        }

        scenario.close()
    }

    @Test
    fun launchWithNmtvChannel() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            // Activity should have launched successfully with the specified channel
            assert(!activity.isFinishing) { "Activity should not be finishing" }
        }

        scenario.close()
    }

    @Test
    fun launchWithClassicsChannel() {
        val scenario = launchWithChannel("nmtv_classics")

        scenario.onActivity { activity ->
            // Activity should have launched successfully with the specified channel
            assert(!activity.isFinishing) { "Activity should not be finishing" }
        }

        scenario.close()
    }

    @Test
    fun launchWithNullChannelUsesDefault() {
        val scenario = launchWithChannel(null)

        scenario.onActivity { activity ->
            // Activity should have launched successfully with default channel
            assert(!activity.isFinishing) { "Activity should not be finishing" }
        }

        scenario.close()
    }

    @Test
    fun playPauseKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        // Simulate pressing play/pause - should not crash
        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, null)
            assert(handled) { "Play/pause key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun dpadUpKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_DPAD_UP, null)
            assert(handled) { "D-pad up key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun dpadDownKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, null)
            assert(handled) { "D-pad down key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun backKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_BACK, null)
            assert(handled) { "Back key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun dpadCenterKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null)
            assert(handled) { "D-pad center key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun mediaPlayKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_MEDIA_PLAY, null)
            assert(handled) { "Media play key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun mediaPauseKeyEventIsHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_MEDIA_PAUSE, null)
            assert(handled) { "Media pause key should be handled" }
        }

        scenario.close()
    }

    @Test
    fun unknownKeyEventIsNotHandled() {
        val scenario = launchWithChannel("nmtv_uk")

        scenario.onActivity { activity ->
            // Volume keys should not be handled by the activity
            val handled = activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, null)
            assert(!handled) { "Volume key should not be handled" }
        }

        scenario.close()
    }

    @Test
    fun activityRecreatesCorrectly() {
        val scenario = launchWithChannel("nmtv_uk")

        // Recreate the activity (simulates config change)
        scenario.recreate()

        // Verify views are still present after recreation
        scenario.onActivity { activity ->
            val playerView = activity.findViewById<androidx.media3.ui.PlayerView>(R.id.playerView)
            assert(playerView != null) { "PlayerView should exist after recreation" }
        }

        scenario.close()
    }
}
