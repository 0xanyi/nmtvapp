package com.nmtv.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for ChannelPortalViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelPortalViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChannelPortalViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChannelPortalViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has channels loaded`() {
        val state = viewModel.uiState.value

        assertEquals(false, state.isLoading)
        assertEquals(2, state.channels.size)
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `getChannels returns all channels`() {
        val channels = viewModel.getChannels()

        assertEquals(2, channels.size)
        assertEquals("nmtv_uk", channels[0].id)
        assertEquals("nmtv_classics", channels[1].id)
    }

    @Test
    fun `getSelectedChannel returns first channel initially`() {
        val selectedChannel = viewModel.getSelectedChannel()

        assertNotNull(selectedChannel)
        assertEquals("nmtv_uk", selectedChannel?.id)
    }

    @Test
    fun `selectChannel updates selected index`() {
        viewModel.selectChannel(1)

        val state = viewModel.uiState.value
        assertEquals(1, state.selectedIndex)
    }

    @Test
    fun `selectChannel ignores invalid index`() {
        viewModel.selectChannel(99)

        val state = viewModel.uiState.value
        assertEquals(0, state.selectedIndex) // Should remain at 0
    }

    @Test
    fun `selectNextChannel wraps around`() {
        // Start at index 0
        assertEquals(0, viewModel.uiState.value.selectedIndex)

        // Move to index 1
        viewModel.selectNextChannel()
        assertEquals(1, viewModel.uiState.value.selectedIndex)

        // Wrap around to index 0
        viewModel.selectNextChannel()
        assertEquals(0, viewModel.uiState.value.selectedIndex)
    }

    @Test
    fun `selectPreviousChannel wraps around`() {
        // Start at index 0
        assertEquals(0, viewModel.uiState.value.selectedIndex)

        // Wrap around to last index
        viewModel.selectPreviousChannel()
        assertEquals(1, viewModel.uiState.value.selectedIndex)

        // Move to index 0
        viewModel.selectPreviousChannel()
        assertEquals(0, viewModel.uiState.value.selectedIndex)
    }

    @Test
    fun `getChannelById returns correct channel`() {
        val channel = viewModel.getChannelById("nmtv_classics")

        assertNotNull(channel)
        assertEquals("NMTV Classics", channel?.name)
    }

    @Test
    fun `getChannelById returns null for unknown id`() {
        val channel = viewModel.getChannelById("unknown_channel")

        assertNull(channel)
    }

    @Test
    fun `getDefaultChannel returns the default channel`() {
        val defaultChannel = viewModel.getDefaultChannel()

        assertNotNull(defaultChannel)
        assertEquals(true, defaultChannel.isDefault)
        assertEquals("nmtv_uk", defaultChannel.id)
    }
}
