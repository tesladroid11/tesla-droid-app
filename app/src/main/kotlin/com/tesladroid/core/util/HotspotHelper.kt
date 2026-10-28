package com.tesladroid.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.tesladroid.engine.ShizukuBridge
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Privileged Wi-Fi Hotspot (SoftAP) and Tethering controller for TeslaDroid.
 *
 * Implements multi-tier tethering start and stop:
 * 1. Privileged TetheringManager invocation via ShizukuBinderWrapper (works on Android 11-15+ including HyperOS)
 * 2. SystemService TetheringManager
 * 3. Legacy ConnectivityManager tethering API
 * 4. Privileged shell command fallback
 */
object HotspotHelper {
    private const val TAG = "HotspotHelper"
    private const val TETHERING_WIFI = 0

    fun getIUserService(): Any? {
        return try {
            val bridgeClass = Class.forName("com.invmaster.teslamirror.ShizukuBridge")
            val field = bridgeClass.getDeclaredField("c")
            field.isAccessible = true
            field.get(null)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to get IUserService from ShizukuBridge: ${t.message}")
            null
        }
    }

    class ShellContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getPackageName(): String = "com.android.shell"
        override fun getOpPackageName(): String = "com.android.shell"
        override fun getAttributionTag(): String? = null

        override fun getApplicationInfo(): ApplicationInfo {
            return super.getApplicationInfo().apply {
                packageName = "com.android.shell"
                targetSdkVersion = Build.VERSION.SDK_INT
            }
        }

        override fun getSystemService(name: String): Any? {
            return super.getSystemService(name)
        }
    }

    /**
     * Checks if Wi-Fi hotspot (SoftAP) is currently active.
     */
    fun isHotspotActive(context: Context): Boolean {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val method = wm?.javaClass?.getDeclaredMethod("isWifiApEnabled")
            method?.isAccessible = true
            val isEnabled = method?.invoke(wm) as? Boolean
            if (isEnabled == true) return true
        } catch (_: Throwable) {}

        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val method = wm?.javaClass?.getDeclaredMethod("getWifiApState")
            method?.isAccessible = true
            val state = (method?.invoke(wm) as? Int) ?: -1
            // 13: WIFI_AP_STATE_ENABLED, 12: WIFI_AP_STATE_ENABLING
            if (state == 13 || state == 12) return true
        } catch (_: Throwable) {}

        if (Shizuku.pingBinder()) {
            try {
                val out = ShizukuBridge.execShell("cmd wifi status")
                if (out.contains("SoftAp", ignoreCase = true) || out.contains("ap enabled", ignoreCase = true)) {
                    return true
                }
            } catch (_: Throwable) {}
        }
        return false
    }

    /**
     * Starts the Wi-Fi hotspot.
     */
    fun startHotspot(context: Context, service: Any? = null): Boolean {
        Log.i(TAG, "Starting Wi-Fi hotspot...")

        // Tier 0: Direct invocation on IUserService (runs as shell UID 2000 with TETHER_PRIVILEGED)
        try {
            val userService = service ?: getIUserService()
            if (userService != null) {
                val method = userService.javaClass.getMethod("tetherStart")
                val res = method.invoke(userService) as? String
                Log.i(TAG, "IUserService.tetherStart result: '$res'")
                if (res != null && !res.startsWith("!")) {
                    Log.i(TAG, "Hotspot started successfully via IUserService.")
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Tier 0 (IUserService.tetherStart) error: ${t.message}", t)
        }

        // Tier 1: TetheringManager with ShizukuBinderWrapper
        if (Shizuku.pingBinder()) {
            try {
                if (startTetheringViaShizuku(context)) {
                    Log.i(TAG, "Hotspot started successfully via Shizuku TetheringManager.")
                    return true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "startTetheringViaShizuku error: ${t.message}", t)
            }
        }

        // Tier 2: SystemService TetheringManager
        try {
            if (startTetheringViaSystemService(context)) {
                Log.i(TAG, "Hotspot started via system service TetheringManager.")
                return true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "startTetheringViaSystemService error: ${t.message}")
        }

        // Tier 3: Legacy ConnectivityManager.startTethering
        try {
            if (startTetheringViaConnectivityManager(context)) {
                Log.i(TAG, "Hotspot started via ConnectivityManager.")
                return true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "startTetheringViaConnectivityManager error: ${t.message}")
        }

        // Tier 4: Shell command via Shizuku
        try {
            val cmd = "cmd connectivity start-tethering wifi || cmd tethering start wifi"
            val res = ShizukuBridge.execShell(cmd)
            Log.i(TAG, "Hotspot shell start executed, output: $res")
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "Shell hotspot start failed: ${t.message}")
        }

        return false
    }

    /**
     * Stops the Wi-Fi hotspot.
     */
    fun stopHotspot(context: Context, service: Any? = null): Boolean {
        Log.i(TAG, "Stopping Wi-Fi hotspot...")
        var stopped = false

        // Tier 0: Direct invocation on IUserService (runs as shell UID 2000 with TETHER_PRIVILEGED)
        try {
            val userService = service ?: getIUserService()
            if (userService != null) {
                val method = userService.javaClass.getMethod("tetherStop")
                val res = method.invoke(userService) as? String
                Log.i(TAG, "IUserService.tetherStop result: '$res'")
                if (res != null && !res.startsWith("!")) {
                    Log.i(TAG, "Hotspot stopped successfully via IUserService.")
                    stopped = true
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Tier 0 (IUserService.tetherStop) error: ${t.message}", t)
        }

        // Tier 1: TetheringManager with ShizukuBinderWrapper
        if (Shizuku.pingBinder()) {
            try {
                if (stopTetheringViaShizuku(context)) {
                    Log.i(TAG, "Hotspot stopped successfully via Shizuku TetheringManager.")
                    stopped = true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "stopTetheringViaShizuku error: ${t.message}", t)
            }
        }

        // Tier 2: SystemService TetheringManager
        try {
            if (stopTetheringViaSystemService(context)) {
                Log.i(TAG, "Hotspot stopped via system service TetheringManager.")
                stopped = true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "stopTetheringViaSystemService error: ${t.message}")
        }

        // Tier 3: Legacy ConnectivityManager.stopTethering
        try {
            if (stopTetheringViaConnectivityManager(context)) {
                Log.i(TAG, "Hotspot stopped via ConnectivityManager.")
                stopped = true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "stopTetheringViaConnectivityManager error: ${t.message}")
        }

        // Tier 4: Shell command
        try {
            val cmd = "cmd connectivity stop-tethering wifi || cmd tethering stop wifi"
            ShizukuBridge.execShell(cmd)
            Log.i(TAG, "Hotspot shell stop executed")
            stopped = true
        } catch (t: Throwable) {
            Log.w(TAG, "Shell hotspot stop failed: ${t.message}")
        }

        return stopped
    }

    private fun getTetheringBinder(): IBinder? {
        return try {
            val sm = Class.forName("android.os.ServiceManager")
            val getService = sm.getMethod("getService", String::class.java)
            getService.invoke(null, "tethering") as? IBinder
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to get tethering binder: ${e.message}")
            null
        }
    }

    private fun getShizukuTetheringManager(context: Context): Any? {
        val rawBinder = getTetheringBinder() ?: return null
        val wrappedBinder = ShizukuBinderWrapper(rawBinder)
        val tmClass = Class.forName("android.net.TetheringManager")

        // Try constructor: TetheringManager(Context, Supplier<IBinder>)
        try {
            val ctor = tmClass.getDeclaredConstructor(Context::class.java, java.util.function.Supplier::class.java)
            ctor.isAccessible = true
            val supplier = java.util.function.Supplier<IBinder> { wrappedBinder }
            return ctor.newInstance(ShellContext(context), supplier)
        } catch (_: Throwable) {}

        // Try constructor: TetheringManager(Context, IBinder)
        try {
            val ctor = tmClass.getDeclaredConstructor(Context::class.java, IBinder::class.java)
            ctor.isAccessible = true
            return ctor.newInstance(ShellContext(context), wrappedBinder)
        } catch (_: Throwable) {}

        return null
    }

    private fun startTetheringViaShizuku(context: Context): Boolean {
        val tm = getShizukuTetheringManager(context) ?: return false
        return invokeStartTethering(tm)
    }

    private fun stopTetheringViaShizuku(context: Context): Boolean {
        val tm = getShizukuTetheringManager(context) ?: return false
        return invokeStopTethering(tm)
    }

    private fun startTetheringViaSystemService(context: Context): Boolean {
        val tm = context.getSystemService("tethering") ?: return false
        return invokeStartTethering(tm)
    }

    private fun stopTetheringViaSystemService(context: Context): Boolean {
        val tm = context.getSystemService("tethering") ?: return false
        return invokeStopTethering(tm)
    }

    private fun invokeStartTethering(tetheringManager: Any): Boolean {
        val tmClass = tetheringManager.javaClass
        val latch = CountDownLatch(1)
        val started = AtomicBoolean(false)

        val callbackClass = Class.forName("android.net.TetheringManager\$StartTetheringCallback")
        val callbackProxy = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            when (method.name) {
                "onTetheringStarted" -> {
                    Log.i(TAG, "onTetheringStarted callback received")
                    started.set(true)
                    latch.countDown()
                }
                "onTetheringFailed" -> {
                    Log.w(TAG, "onTetheringFailed callback: ${args?.contentToString()}")
                    latch.countDown()
                }
            }
            null
        }

        val directExecutor = Executor { it.run() }

        // Android 11+ TetheringRequest approach
        try {
            val builderClass = Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
            val builder = builderClass.getConstructor(Int::class.javaPrimitiveType).newInstance(TETHERING_WIFI)
            try {
                builderClass.getMethod("setShouldShowEntitlementUi", Boolean::class.javaPrimitiveType).invoke(builder, false)
            } catch (_: Throwable) {}
            val request = builderClass.getMethod("build").invoke(builder)

            val method = tmClass.getMethod("startTethering", request.javaClass, Executor::class.java, callbackClass)
            method.invoke(tetheringManager, request, directExecutor, callbackProxy)
            latch.await(3, TimeUnit.SECONDS)
            return true
        } catch (e: Throwable) {
            Log.d(TAG, "TetheringRequest approach fallback: ${e.message}")
        }

        // Legacy startTethering(int, Executor, StartTetheringCallback)
        try {
            val method = tmClass.getMethod("startTethering", Int::class.javaPrimitiveType, Executor::class.java, callbackClass)
            method.invoke(tetheringManager, TETHERING_WIFI, directExecutor, callbackProxy)
            latch.await(3, TimeUnit.SECONDS)
            return true
        } catch (e: Throwable) {
            Log.w(TAG, "Legacy startTethering failed: ${e.message}")
        }

        return false
    }

    private fun invokeStopTethering(tetheringManager: Any): Boolean {
        val tmClass = tetheringManager.javaClass
        try {
            val method = tmClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
            method.invoke(tetheringManager, TETHERING_WIFI)
            return true
        } catch (e: Throwable) {
            Log.w(TAG, "stopTethering invocation error: ${e.message}")
            return false
        }
    }

    private fun startTetheringViaConnectivityManager(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) ?: return false
        val callbackClass = try {
            Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
        } catch (_: Throwable) {
            return false
        }
        val callbackProxy = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, _ ->
            Log.i(TAG, "ConnectivityManager OnStartTetheringCallback: ${method.name}")
            null
        }
        val method = cm.javaClass.getMethod("startTethering", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, callbackClass)
        method.invoke(cm, TETHERING_WIFI, false, callbackProxy)
        return true
    }

    private fun stopTetheringViaConnectivityManager(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) ?: return false
        val method = cm.javaClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
        method.invoke(cm, TETHERING_WIFI)
        return true
    }
}
