package com.nmtv.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.nmtv.app.MainActivity
import com.nmtv.app.R
import com.nmtv.app.data.model.Channel
import com.nmtv.app.data.repository.ChannelRepository
import com.nmtv.app.data.repository.LocalChannelRepository

/**
 * Leanback BrowseFragment for displaying channel categories and cards.
 * Provides proper focus navigation and accessibility for Android TV.
 */
class BrowseChannelsFragment : BrowseSupportFragment() {

    private lateinit var channelRepository: ChannelRepository
    private lateinit var rowsAdapter: ArrayObjectAdapter

    companion object {
        private const val TAG = "BrowseChannelsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        channelRepository = LocalChannelRepository()

        setupUIElements()
        loadChannels()
        setupEventListeners()
    }

    private fun setupUIElements() {
        // Set up the header and branding
        title = getString(R.string.app_name)
        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = false

        // Set brand color
        brandColor = resources.getColor(R.color.brand_primary, null)

        // Set search affordance color (if search is enabled)
        searchAffordanceColor = resources.getColor(R.color.brand_accent, null)
    }

    private fun loadChannels() {
        val channels = channelRepository.getAllChannels()
        Log.d(TAG, "Loaded ${channels.size} channels")

        // Create the main rows adapter
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        // Create "Live Channels" row
        val liveChannelsHeader = HeaderItem(0, "Live Channels")
        val liveChannelsAdapter = ArrayObjectAdapter(ChannelPresenter())

        // Add all channels (in production, you'd filter by category)
        channels.forEach { channel ->
            liveChannelsAdapter.add(channel)
        }

        rowsAdapter.add(ListRow(liveChannelsHeader, liveChannelsAdapter))

        // Set the adapter
        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        // Handle item clicks
        onItemViewClickedListener = OnItemViewClickedListener {
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row? ->

            if (item is Channel) {
                Log.d(TAG, "Channel clicked: ${item.name}")
                launchChannel(item.id)
            }
        }

        // Handle item selection (focus changes)
        onItemViewSelectedListener = OnItemViewSelectedListener {
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row? ->

            if (item is Channel) {
                Log.d(TAG, "Channel selected: ${item.name}")
                // Could update background or show preview here
            }
        }
    }

    private fun launchChannel(channelId: String) {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            putExtra("channel_id", channelId)
        }
        startActivity(intent)
    }
}
