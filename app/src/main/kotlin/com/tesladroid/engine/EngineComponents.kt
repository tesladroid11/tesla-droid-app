package com.tesladroid.engine

import android.content.Context
import android.util.Log

/**
 * Privileged user service running under Shizuku / ADB context.
 */
class UserService {
    fun tetherStart(context: Context? = null): String {
        return try {
            val ctx = context ?: com.tesladroid.TeslaDroidApp.instance
            if (com.tesladroid.core.util.HotspotHelper.startHotspot(ctx)) "" else "!failed to start tethering"
        } catch (e: Throwable) {
            "!${e.message}"
        }
    }

    fun tetherStop(context: Context? = null): String {
        return try {
            val ctx = context ?: com.tesladroid.TeslaDroidApp.instance
            if (com.tesladroid.core.util.HotspotHelper.stopHotspot(ctx)) "" else "!failed to stop tethering"
        } catch (e: Throwable) {
            "!${e.message}"
        }
    }

    fun softApForce5() {}

    fun shell(command: String): String {
        return ShizukuBridge.execShell(command)
    }
}

/**
 * Bridge interface for communicating with Shizuku IPC system services.
 */
class ShizukuBridge {
    fun bindUserService(context: Context, callback: ShizukuCallback) {
        val userService = UserService()
        currentUserService = userService
        try {
            callback.onServiceReady(userService, null)
        } catch (t: Throwable) {
            Log.e("ShizukuBridge", "Error invoking ShizukuCallback", t)
        }
    }

    companion object {
        @JvmField
        var instance: ShizukuBridge = ShizukuBridge()

        @JvmField
        var currentUserService: Any? = UserService()

        @JvmStatic
        fun executeCommand(context: Context, cmd: String): String {
            return execShell(cmd)
        }

        @JvmStatic
        fun b(context: Context, cmd: String): String {
            return execShell(cmd)
        }

        @JvmStatic
        fun execShell(command: String): String {
            return try {
                val proc: Process = if (rikka.shizuku.Shizuku.pingBinder()) {
                    try {
                        val method = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                            "newProcess",
                            Array<String>::class.java,
                            Array<String>::class.java,
                            String::class.java
                        ).apply { isAccessible = true }
                        method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                    } catch (_: Throwable) {
                        Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                    }
                } else {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                }
                val output = proc.inputStream.bufferedReader().use { it.readText() }
                proc.waitFor()
                output
            } catch (e: Throwable) {
                Log.w("ShizukuBridge", "execShell error: ${e.message}")
                ""
            }
        }
    }
}

/**
 * Local HTTP/WebSocket streaming server component.
 */
class StreamServer {
    companion object {
        @JvmField
        var sessionToken: String? = null

        @JvmField
        var connectedClients: List<Any>? = emptyList()

        @JvmField
        var currentFps: Int = 30

        @JvmField
        var videoDelayMs: Int = 0

        @Volatile
        var isRunning: Boolean = false

        @JvmStatic
        fun updateAudioMode(context: Context) {}

        @JvmStatic
        fun applyVideoDelay() {}
    }
}

/**
 * Bluetooth connection state provider for car integration.
 */
class CarBluetooth {
    companion object {
        @JvmField
        var isConnected: Boolean = false
    }
}

/**
 * Preferences accessor for engine components.
 */
class Prefs {
    companion object {
        @JvmStatic
        fun getSessionToken(context: Context): String? = null
    }
}

/**
 * TLS certificate host provider.
 */
class CertStore {
    companion object {
        @JvmStatic
        fun getCertName(context: Context): String? = null
    }
}

/**
 * Session termination and cleanup handler.
 */
class SessionStop {
    companion object {
        @JvmField
        var shouldStopTether: Boolean = false

        @JvmStatic
        fun cleanup(context: Context) {}
    }
}

/**
 * Dynamic bitrate controller for video streaming.
 */
class AutoBitrate {
    companion object {
        @JvmField
        var currentBitrateMbps: Int = 0
    }
}

/**
 * GPS Shield provider (Anti-Jamming & Anti-Spoofing protection).
 */
class MockGps {
    fun updateGpsShield(context: Context, reason: String) {}
    fun stopGpsShield(context: Context) {}

    companion object {
        @JvmField
        var instance: MockGps? = MockGps()
    }
}

/**
 * In-car dashboard trip and odometer telemetry bridge.
 */
class Odometer {
    fun setOdometerEnabled(context: Context, src: String, enabled: Boolean) {}
    fun notifyOdometerState(enabled: Boolean) {}

    companion object {
        @JvmField
        var instance: Odometer? = Odometer()
    }
}

/**
 * Wake word detector ("Hey Tesla").
 */
class WakeWord {
    companion object {
        @JvmStatic
        fun startListening(service: Any) {}

        @JvmStatic
        fun stopListening(src: String) {}
    }
}

/**
 * Screen dimming overlay manager for battery and OLED preservation.
 */
class SystemDim {
    companion object {
        @JvmStatic
        fun dimScreen(reason: String): Boolean = false

        @JvmStatic
        fun restoreScreen() {}
    }
}
