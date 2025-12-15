package com.nmtv.app.data.repository

import com.nmtv.app.data.model.Channel

/**
 * Repository interface for managing TV channels.
 * This abstraction allows for easy extensibility and testing.
 */
interface ChannelRepository {
    /**
     * Get the default channel to play on app launch.
     */
    fun getDefaultChannel(): Channel

    /**
     * Get a channel by its ID.
     * @return Channel if found, null otherwise
     */
    fun getChannelById(id: String): Channel?

    /**
     * Get all available channels.
     */
    fun getAllChannels(): List<Channel>

    /**
     * Get the next channel in the list (for channel up navigation).
     * @param currentId ID of the current channel
     * @return Next channel, or wraps to first channel if at end
     */
    fun getNextChannel(currentId: String): Channel?

    /**
     * Get the previous channel in the list (for channel down navigation).
     * @param currentId ID of the current channel
     * @return Previous channel, or wraps to last channel if at beginning
     */
    fun getPreviousChannel(currentId: String): Channel?
}