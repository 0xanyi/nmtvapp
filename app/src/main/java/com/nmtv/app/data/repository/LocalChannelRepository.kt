package com.nmtv.app.data.repository

import com.nmtv.app.data.model.Channel

/**
 * Local implementation of ChannelRepository with hardcoded channels.
 * To add more channels, simply add them to the channels list below.
 */
class LocalChannelRepository : ChannelRepository {

    private val channels = listOf(
        Channel(
            id = "nmtv_uk",
            name = "NMTV UK",
            streamUrl = "https://cdn3.wowza.com/5/L1Uzd2FrbVlLRG1W/live/smil:nmtvuk.smil/playlist.m3u8",
            isDefault = true
        ),
        Channel(
            id = "nmtv_classics",
            name = "NMTV Classics",
            streamUrl = "https://cdn3.wowza.com/5/NVF5TVdNQmR5OHRI/nwmc/nwmc_hd/playlist.m3u8",
            isDefault = false
        )
    )

    override fun getDefaultChannel(): Channel {
        return channels.firstOrNull { it.isDefault } 
            ?: channels.first()
    }

    override fun getChannelById(id: String): Channel? {
        return channels.firstOrNull { it.id == id }
    }

    override fun getAllChannels(): List<Channel> {
        return channels
    }

    override fun getNextChannel(currentId: String): Channel? {
        val currentIndex = channels.indexOfFirst { it.id == currentId }
        if (currentIndex == -1) return null
        
        val nextIndex = (currentIndex + 1) % channels.size
        return channels[nextIndex]
    }

    override fun getPreviousChannel(currentId: String): Channel? {
        val currentIndex = channels.indexOfFirst { it.id == currentId }
        if (currentIndex == -1) return null
        
        val previousIndex = if (currentIndex == 0) {
            channels.size - 1
        } else {
            currentIndex - 1
        }
        return channels[previousIndex]
    }
}