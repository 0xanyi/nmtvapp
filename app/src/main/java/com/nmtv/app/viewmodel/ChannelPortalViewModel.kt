package com.nmtv.app.viewmodel

import androidx.lifecycle.ViewModel
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for channel selection in the portal.
 * Manages channel list and selection state.
 */
class ChannelPortalViewModel : ViewModel() {

    private val channelRepository: ChannelRepository = LocalChannelRepository()

    /**
     * Represents the UI state for the channel portal
     */
    data class UiState(
        val channels: List<Channel> = emptyList(),
        val selectedIndex: Int = 0,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        val channels = channelRepository.getAllChannels()
        _uiState.value = UiState(
            channels = channels,
            selectedIndex = 0,
            isLoading = false
        )
    }

    /**
     * Get all available channels
     */
    fun getChannels(): List<Channel> {
        return _uiState.value.channels
    }

    /**
     * Get currently selected channel
     */
    fun getSelectedChannel(): Channel? {
        val state = _uiState.value
        return state.channels.getOrNull(state.selectedIndex)
    }

    /**
     * Update the selected channel index
     */
    fun selectChannel(index: Int) {
        val state = _uiState.value
        if (index in state.channels.indices) {
            _uiState.value = state.copy(selectedIndex = index)
        }
    }

    /**
     * Select next channel (with wrap-around)
     */
    fun selectNextChannel() {
        val state = _uiState.value
        if (state.channels.isNotEmpty()) {
            val nextIndex = (state.selectedIndex + 1) % state.channels.size
            _uiState.value = state.copy(selectedIndex = nextIndex)
        }
    }

    /**
     * Select previous channel (with wrap-around)
     */
    fun selectPreviousChannel() {
        val state = _uiState.value
        if (state.channels.isNotEmpty()) {
            val prevIndex = if (state.selectedIndex == 0) {
                state.channels.size - 1
            } else {
                state.selectedIndex - 1
            }
            _uiState.value = state.copy(selectedIndex = prevIndex)
        }
    }

    /**
     * Get channel by ID
     */
    fun getChannelById(id: String): Channel? {
        return channelRepository.getChannelById(id)
    }

    /**
     * Get the default channel
     */
    fun getDefaultChannel(): Channel {
        return channelRepository.getDefaultChannel()
    }
}
