package com.tesladroid

import android.app.Application

class TeslaDroidApp : Application() {

    companion object {
        lateinit var instance: TeslaDroidApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            com.tesladroid.core.util.AppPreferences(this).syncAllToTeslaMirror()
        } catch (_: Throwable) {}
        try {
            com.tesladroid.core.receiver.WifiAutoStartManager.start(this)
        } catch (t: Throwable) {
            android.util.Log.w("TeslaDroidApp", "Failed to start WifiAutoStartManager: ${t.message}")
        }
    }
}
