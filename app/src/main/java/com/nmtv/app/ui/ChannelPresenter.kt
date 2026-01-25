package com.nmtv.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.nmtv.app.R
import com.nmtv.app.data.model.Channel

/**
 * Presenter for channel cards in Leanback BrowseFragment.
 * Handles rendering and focus animations for channel cards.
 */
class ChannelPresenter : Presenter() {

    companion object {
        private const val FOCUS_SCALE = 1.08f
        private const val FOCUS_DURATION = 200L
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_card, parent, false)

        // Set up focus change listener for scale animation
        view.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) FOCUS_SCALE else 1.0f
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(FOCUS_DURATION)
                .start()
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val channel = item as? Channel ?: return
        val view = viewHolder.view

        val previewImage = view.findViewById<ImageView>(R.id.channelPreview)
        val titleText = view.findViewById<TextView>(R.id.channelTitle)
        val descText = view.findViewById<TextView>(R.id.channelDescription)

        titleText.text = channel.name
        descText.text = getChannelDescription(channel)

        // Set preview image based on channel ID
        val previewResId = getPreviewResource(channel.id)
        if (previewResId != 0) {
            previewImage.setImageResource(previewResId)
        }

        // Accessibility
        view.contentDescription = "${channel.name}. ${getChannelDescription(channel)}"
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val view = viewHolder.view
        view.findViewById<ImageView>(R.id.channelPreview)?.setImageDrawable(null)
    }

    private fun getChannelDescription(channel: Channel): String {
        return when (channel.id) {
            "nmtv_uk" -> "Live streaming from the UK with the latest content"
            "nmtv_classics" -> "Classic shows and timeless entertainment"
            else -> "Stream now"
        }
    }

    private fun getPreviewResource(channelId: String): Int {
        return when (channelId) {
            "nmtv_uk" -> R.drawable.nmtv_live
            "nmtv_classics" -> R.drawable.nmtv_classics
            else -> 0
        }
    }
}
