package com.nmtv.app.player

import androidx.media3.common.PlaybackException

/**
 * Listener interface for communicating player state changes to the UI.
 */
interface StreamPlayerListener {
    /**
     * Called when the player is buffering content.
     */
    fun onBuffering()

    /**
     * Called when the player starts playing content.
     */
    fun onPlaying()

    /**
     * Called when a playback error occurs.
     * @param error The exception that caused the error
     */
    fun onError(error: PlaybackException)

    /**
     * Called when playback reaches the end of the stream.
     */
    fun onEnded()

    /**
     * Called when playback is paused.
     */
    fun onPaused()
}