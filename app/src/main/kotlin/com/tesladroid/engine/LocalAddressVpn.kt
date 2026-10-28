package com.tesladroid.engine

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class LocalAddressVpn : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.tesladroid.engine.VPN_STOP") {
            isRunning = false
            try {
                vpnInterface?.close()
                vpnInterface = null
            } catch (_: Throwable) {}
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        try {
            vpnInterface = Builder()
                .setSession("TeslaDroid Local VPN")
                .addAddress("100.99.9.9", 30)
                .establish()
        } catch (e: Throwable) {
            Log.w("LocalAddressVpn", "Establish error: ${e.message}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Throwable) {}
    }

    companion object {
        @JvmField
        var isRunning: Boolean = false
    }
}
