package com.nmtv.app

import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for ChannelPortalActivity focus navigation.
 * Tests D-pad navigation and card focus handling.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ChannelPortalActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ChannelPortalActivity::class.java)

    @Test
    fun channelCardsAreDisplayed() {
        onView(withId(R.id.cardNmtv))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardClassics))
            .check(matches(isDisplayed()))
    }

    @Test
    fun initialFocusIsOnFirstCard() {
        onView(withId(R.id.cardNmtv))
            .check(matches(hasFocus()))
    }

    @Test
    fun dpadRightMovesFocusToSecondCard() {
        // Start with first card focused
        onView(withId(R.id.cardNmtv))
            .check(matches(hasFocus()))

        // Press D-pad right
        onView(withId(R.id.cardNmtv))
            .perform(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))

        // Second card should now have focus
        onView(withId(R.id.cardClassics))
            .check(matches(hasFocus()))
    }

    @Test
    fun dpadLeftMovesFocusToFirstCard() {
        // Navigate to second card first
        onView(withId(R.id.cardNmtv))
            .perform(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))

        // Verify second card has focus
        onView(withId(R.id.cardClassics))
            .check(matches(hasFocus()))

        // Press D-pad left
        onView(withId(R.id.cardClassics))
            .perform(pressKey(KeyEvent.KEYCODE_DPAD_LEFT))

        // First card should now have focus
        onView(withId(R.id.cardNmtv))
            .check(matches(hasFocus()))
    }

    @Test
    fun cardTitlesAreDisplayed() {
        onView(withId(R.id.cardNmtvTitle))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardClassicsTitle))
            .check(matches(isDisplayed()))
    }

    @Test
    fun cardDescriptionsAreDisplayed() {
        onView(withId(R.id.cardNmtvDesc))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardClassicsDesc))
            .check(matches(isDisplayed()))
    }

    @Test
    fun cardPreviewImagesAreDisplayed() {
        onView(withId(R.id.cardNmtvPreview))
            .check(matches(isDisplayed()))

        onView(withId(R.id.cardClassicsPreview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun backgroundElementsAreDisplayed() {
        onView(withId(R.id.blurBackground))
            .check(matches(isDisplayed()))
    }

    @Test
    fun reflectionViewsExist() {
        // Reflections may not be visible initially but should exist
        activityRule.scenario.onActivity { activity ->
            val nmtvReflection = activity.findViewById<android.widget.ImageView>(R.id.cardNmtvReflection)
            val classicsReflection = activity.findViewById<android.widget.ImageView>(R.id.cardClassicsReflection)

            assert(nmtvReflection != null) { "NMTV reflection view should exist" }
            assert(classicsReflection != null) { "Classics reflection view should exist" }
        }
    }

    @Test
    fun focusChangesUpdateCardScale() {
        activityRule.scenario.onActivity { activity ->
            val cardNmtv = activity.findViewById<android.view.View>(R.id.cardNmtv)
            val cardClassics = activity.findViewById<android.view.View>(R.id.cardClassics)

            // Initially first card should be focused (scaled up)
            assert(cardNmtv.hasFocus()) { "First card should have initial focus" }

            // Give time for animation
            Thread.sleep(350)

            // First card should be scaled up when focused
            assert(cardNmtv.scaleX > 1.0f) { "Focused card should be scaled up" }
            assert(cardClassics.scaleX == 1.0f) { "Unfocused card should have normal scale" }
        }
    }

    @Test
    fun dpadNavigationCycleWorks() {
        // Start at first card
        onView(withId(R.id.cardNmtv))
            .check(matches(hasFocus()))

        // Navigate right -> second card
        onView(withId(R.id.cardNmtv))
            .perform(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))

        onView(withId(R.id.cardClassics))
            .check(matches(hasFocus()))

        // Navigate left -> back to first card
        onView(withId(R.id.cardClassics))
            .perform(pressKey(KeyEvent.KEYCODE_DPAD_LEFT))

        onView(withId(R.id.cardNmtv))
            .check(matches(hasFocus()))
    }

    @Test
    fun cardClickLaunchesMainActivity() {
        // This test verifies that clicking a card attempts to launch MainActivity
        // We can't fully verify the navigation without mocking, but we can check
        // the click handler is set up
        activityRule.scenario.onActivity { activity ->
            val cardNmtv = activity.findViewById<android.view.View>(R.id.cardNmtv)
            assert(cardNmtv.hasOnClickListeners()) { "Card should have click listener" }
        }
    }
}
