package com.nmtv.app.data.model

import kotlinx.serialization.Serializable

/**
 * Enum representing the source of video content.
 */
@Serializable
enum class ContentSource {
    BIBLE_BRAIN,
    JESUS_FILM,
    YOUTUBE,
    CUSTOM
}

/**
 * Data class representing on-demand video content.
 *
 * @property id Unique identifier for the video
 * @property title Display title of the video
 * @property description Detailed description of the video content
 * @property thumbnailUrl URL to the video's thumbnail image
 * @property videoUrl URL to the video stream (HLS or direct)
 * @property duration Duration in seconds
 * @property source Source of the video content
 * @property category Content category
 * @property language ISO 639-3 language code (e.g., "eng", "spa")
 * @property publishedAt ISO 8601 timestamp of when the video was published
 */
@Serializable
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val duration: Int,
    val source: ContentSource,
    val category: String,
    val language: String? = null,
    val publishedAt: String? = null
)
