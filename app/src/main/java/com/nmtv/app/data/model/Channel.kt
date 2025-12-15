package com.nmtv.app.data.model

/**
 * Data class representing a TV channel with HLS streaming information.
 *
 * @property id Unique identifier for the channel
 * @property name Display name of the channel
 * @property streamUrl HLS stream URL (m3u8 playlist)
 * @property isDefault Whether this is the default channel to play on app launch
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val isDefault: Boolean = false
)