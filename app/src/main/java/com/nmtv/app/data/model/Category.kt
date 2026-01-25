package com.nmtv.app.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing a content category.
 *
 * @property id Unique identifier for the category
 * @property name Display name of the category
 * @property description Description of the category
 * @property iconUrl Optional icon URL for the category
 * @property order Display order (lower numbers appear first)
 */
@Serializable
data class Category(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    val order: Int = 0
)
