package com.nmtv.app.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing a stream from the NMTV API.
 * Matches the backend Stream model structure.
 *
 * @property id Unique identifier for the stream
 * @property name Display name of the stream
 * @property description Detailed description of the stream content
 * @property thumbnailUrl URL to the stream's thumbnail image
 * @property hlsUrl HLS stream URL (m3u8 playlist)
 * @property isLive Whether this is a live stream or on-demand content
 * @property category Content category (e.g., "live", "classics", "bible-study")
 */
@Serializable
data class Stream(
    val id: String,
    val name: String,
    val description: String,
    val thumbnailUrl: String,
    val hlsUrl: String,
    val isLive: Boolean,
    val category: String
) {
    /**
     * Converts this Stream to a Channel for compatibility with existing UI code.
     */
    fun toChannel(): Channel {
        return Channel(
            id = id,
            name = name,
            streamUrl = hlsUrl,
            isDefault = false
        )
    }
}
