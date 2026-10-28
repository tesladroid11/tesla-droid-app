package com.tesladroid.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.tesladroid.core.engine.CoreEngineBridge
import com.tesladroid.core.model.ConnectionState
import com.tesladroid.core.util.AppPreferences
import rikka.shizuku.Shizuku

object WifiAutoStartManager {
    private const val TAG = "WifiAutoStart"
    private var isStarted = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var dynamicReceiver: BroadcastReceiver? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var disconnectRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null
    var userManuallyStopped: Boolean = false

    private fun acquireWakeLock(context: Context, timeoutMs: Long = 35000L) {
        try {
            if (wakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TeslaDroid:wifiDisconnectStop")
                wakeLock?.setReferenceCounted(false)
            }
            wakeLock?.acquire(timeoutMs)
            Log.i(TAG, "WakeLock acquired for $timeoutMs ms")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to acquire wake lock: ${t.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "WakeLock released")
            }
        } catch (_: Throwable) {}
    }

    fun cancelDisconnectTimer() {
        if (disconnectRunnable != null) {
            Log.i(TAG, "Cancelling pending Wi-Fi disconnect auto-stop timer.")
            disconnectRunnable?.let { mainHandler.removeCallbacks(it) }
            disconnectRunnable = null
            releaseWakeLock()
        }
    }

    fun start(context: Context) {
        if (isStarted) return
        isStarted = true
        val appContext = context.applicationContext

        Log.i(TAG, "Starting WifiAutoStartManager...")

        // 1. Dynamic BroadcastReceiver for Wi-Fi and unlock events
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val action = intent.action ?: return
                Log.d(TAG, "Dynamic broadcast received: $action")
                checkAndTrigger(ctx)
            }
        }
        dynamicReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            appContext.registerReceiver(receiver, filter)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register dynamic receiver: ${t.message}")
        }

        // 2. ConnectivityManager.NetworkCallback
        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                val cb = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "NetworkCallback: Wi-Fi onAvailable")
                        mainHandler.postDelayed({ checkAndTrigger(appContext) }, 500L)
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        Log.i(TAG, "NetworkCallback: Wi-Fi onCapabilitiesChanged")
                        checkAndTrigger(appContext, networkCapabilities)
                    }

                    override fun onLost(network: Network) {
                        Log.i(TAG, "NetworkCallback: Wi-Fi onLost")
                        mainHandler.postDelayed({ checkAndTrigger(appContext) }, 500L)
                    }
                }
                networkCallback = cb
                cm.registerNetworkCallback(request, cb)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register NetworkCallback: ${t.message}")
        }

        // 3. Immediate check on start
        mainHandler.postDelayed({ checkAndTrigger(appContext) }, 1000L)
    }

    fun stop(context: Context) {
        if (!isStarted) return
        isStarted = false
        val appContext = context.applicationContext
        cancelDisconnectTimer()
        try {
            dynamicReceiver?.let { appContext.unregisterReceiver(it) }
        } catch (_: Throwable) {}
        dynamicReceiver = null

        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Throwable) {}
        networkCallback = null
    }

    fun getConnectedWifiSsid(context: Context, capabilities: NetworkCapabilities? = null): String? {
        // Tier 1: From NetworkCapabilities
        try {
            val caps = capabilities ?: run {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val net = cm?.activeNetwork
                if (net != null) cm.getNetworkCapabilities(net) else null
            }
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiInfo = caps.transportInfo as? WifiInfo
                val s = wifiInfo?.ssid?.trim()?.removeSurrounding("\"")
                if (!s.isNullOrBlank() && s != "<unknown ssid>") return s
            }
        } catch (_: Throwable) {}

        // Tier 2: WifiManager connectionInfo
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val s = wm?.connectionInfo?.ssid?.trim()?.removeSurrounding("\"")
            if (!s.isNullOrBlank() && s != "<unknown ssid>") return s
        } catch (_: Throwable) {}

        // Tier 3: Shizuku / Shell fallback (cmd wifi status)
        try {
            val out = com.tesladroid.engine.ShizukuBridge.executeCommand(context, "cmd wifi status")
            if (out.isNotBlank()) {
                val match = Regex("""Wifi is connected to "([^"]+)"""").find(out)
                    ?: Regex("""SSID: "([^"]+)"""").find(out)
                if (match != null) {
                    val s = match.groupValues[1].trim()
                    if (s.isNotBlank() && s != "<unknown ssid>") return s
                }
            }
        } catch (_: Throwable) {}

        try {
            if (Shizuku.pingBinder()) {
                val m = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                m.isAccessible = true
                val p = m.invoke(null, arrayOf("cmd", "wifi", "status"), null, null) as? Process
                if (p != null) {
                    val out = p.inputStream.bufferedReader().readText()
                    p.waitFor()
                    val match = Regex("""Wifi is connected to "([^"]+)"""").find(out)
                        ?: Regex("""SSID: "([^"]+)"""").find(out)
                    if (match != null) {
                        val s = match.groupValues[1].trim()
                        if (s.isNotBlank() && s != "<unknown ssid>") return s
                    }
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    fun checkAndTrigger(context: Context, capabilities: NetworkCapabilities? = null) {
        val prefs = AppPreferences(context)
        val bridge = CoreEngineBridge.instance

        if (!prefs.wifiAutoStart) {
            cancelDisconnectTimer()
            Log.d(TAG, "Wi-Fi auto-start disabled in preferences.")
            return
        }

        val targetConfig = prefs.wifiAutoStartSsid.trim()
        if (targetConfig.isBlank()) {
            cancelDisconnectTimer()
            Log.d(TAG, "Wi-Fi auto-start target SSID is blank.")
            return
        }

        val targets = targetConfig.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
        val currentSsid = getConnectedWifiSsid(context, capabilities)

        val isTargetConnected = currentSsid != null && targets.any { it.equals(currentSsid, ignoreCase = true) }
        Log.i(TAG, "Current Wi-Fi: '$currentSsid', Configured targets: $targets, isTargetConnected=$isTargetConnected, streamingState=${bridge.connectionState.value}")

        if (isTargetConnected) {
            // Cancel any pending disconnect auto-stop
            cancelDisconnectTimer()

            if (userManuallyStopped) {
                Log.i(TAG, "Streaming was stopped manually by user on '$currentSsid'. Skipping auto-trigger.")
                return
            }

            if (bridge.connectionState.value != ConnectionState.IDLE) {
                Log.i(TAG, "Streaming already in progress (${bridge.connectionState.value}), skipping trigger.")
                return
            }

            Log.i(TAG, "MATCH! Target Wi-Fi '$currentSsid' connected! Triggering TeslaDroid streaming...")

            // Mark pending so MainActivity knows auto-start was requested
            val sp = context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
            sp.edit().putLong("car_bt_pending_at", System.currentTimeMillis()).apply()

            // 1. If Shizuku is ready and VPN consent was already granted, start streaming directly
            try {
                if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    if (bridge.checkVpnConsentRequired(context) == null) {
                        Log.i(TAG, "Directly starting Shizuku streaming from background...")
                        bridge.startShizukuStreaming(context)
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Direct streaming start error: ${t.message}")
            }

            // 2. Launch MainActivity to bring UI to foreground / handle VPN consent if needed
            try {
                val intent = Intent(context, com.tesladroid.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("auto_start_source", "wifi")
                }
                context.startActivity(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to start MainActivity: ${t.message}")
            }
        } else {
            // Target Wi-Fi is NOT connected
            userManuallyStopped = false
            if (bridge.connectionState.value != ConnectionState.IDLE) {
                if (disconnectRunnable == null) {
                    val stopDelaySec = if (prefs.autoStopSeconds > 0) prefs.autoStopSeconds.toLong() else 30L
                    val stopDelayMs = stopDelaySec * 1000L
                    Log.i(TAG, "Target Wi-Fi disconnected while streaming! Holding wake lock and waiting $stopDelaySec seconds before auto-stop...")
                    acquireWakeLock(context, stopDelayMs + 5000L)

                    val r = Runnable {
                        Log.i(TAG, "$stopDelaySec seconds without target Wi-Fi elapsed. Auto-stopping streaming session!")
                        bridge.stopStreaming(context)
                        bridge.dismissSystemDim()
                        releaseWakeLock()
                        disconnectRunnable = null
                    }
                    disconnectRunnable = r
                    mainHandler.postDelayed(r, stopDelayMs)
                }
            } else {
                cancelDisconnectTimer()
            }
        }
    }
}

class WifiAutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WifiAutoStartManager.checkAndTrigger(context)
    }
}
