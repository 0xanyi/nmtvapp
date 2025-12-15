package com.nmtv.app

import android.app.Application
import android.util.Log

/**
 * Application class for NMTV.
 * Used for app-wide initialization if needed in the future.
 */
class TvStreamApp : Application() {

    companion object {
        private const val TAG = "TvStreamApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NMTV Application started")
    }
}