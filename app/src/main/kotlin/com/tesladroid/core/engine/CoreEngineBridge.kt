package com.tesladroid.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.tesladroid.core.model.AudioMode
import com.tesladroid.core.model.ConnectionState
import com.tesladroid.core.model.StreamQuality
import com.tesladroid.core.util.AppPreferences
import com.tesladroid.engine.AutoBitrate
import com.tesladroid.engine.CaptureService
import com.tesladroid.engine.CertStore
import com.tesladroid.engine.LocalAddressVpn
import com.tesladroid.engine.MockGps
import com.tesladroid.engine.Odometer
import com.tesladroid.engine.Prefs
import com.tesladroid.engine.SessionStop
import com.tesladroid.engine.ShizukuBridge
import com.tesladroid.engine.StreamServer
import com.tesladroid.engine.SystemDim
import com.tesladroid.engine.UserService
import com.tesladroid.engine.WakeWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Core Engine Bridge connects the UI layer to the native streaming subsystem:
 * - MediaProjection & hardware MediaCodec (H.264 / H.265)
 * - Shizuku / ADB Virtual Display creation & Touch Input Injection (/dev/input / uinput)
 * - Local HTTPS/WSS Server on port 8443 with TLS (*.car.tesla-droid.app / 100.99.9.9)
 * - LocalAddressVpn (100.99.9.9 network interface)
 */
class CoreEngineBridge private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null
    private var appWatcherJob: Job? = null
    private var btReceiver: BroadcastReceiver? = null
    private var btDisconnectJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var streamingWakeLock: PowerManager.WakeLock? = null

    // Fullscreen dim overlay window (no navigation bar)
    private var dimView: View? = null
    private var isDimActive: Boolean = false

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _streamQuality = MutableStateFlow(StreamQuality.FHD_1080P)
    val streamQuality: StateFlow<StreamQuality> = _streamQuality.asStateFlow()

    private val _audioMode = MutableStateFlow(AudioMode.TESLA_BT)
    val audioMode: StateFlow<AudioMode> = _audioMode.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://car.tesla-droid.app:8443")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    private val _shortUrl = MutableStateFlow<String?>(null)
    val shortUrl: StateFlow<String?> = _shortUrl.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _currentFps = MutableStateFlow(0)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()

    private val _currentBitrateMbps = MutableStateFlow(0.0f)
    val currentBitrateMbps: StateFlow<Float> = _currentBitrateMbps.asStateFlow()

    private val _streamingDurationSeconds = MutableStateFlow(0L)
    val streamingDurationSeconds: StateFlow<Long> = _streamingDurationSeconds.asStateFlow()

    fun updateStreamQuality(quality: StreamQuality) {
        _streamQuality.value = quality
    }

    fun updateAudioMode(mode: AudioMode) {
        _audioMode.value = mode
    }

    fun updateBitrate(mbps: Int) {
        if (mbps > 0) {
            _currentBitrateMbps.value = mbps.toFloat()
        }
    }

    fun checkVpnConsentRequired(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun getNativeCaptureService(): Any? {
        return try {
            val cls = Class.forName("com.invmaster.teslamirror.CaptureService")
            for (fieldName in listOf("o1", "s1", "n1", "w1")) {
                try {
                    val obj = cls.getField(fieldName).get(null)
                    if (obj != null && cls.isInstance(obj)) return obj
                } catch (_: Throwable) {}
            }
            CaptureService.instance
        } catch (_: Throwable) {
            CaptureService.instance
        }
    }

    fun startShizukuStreaming(context: Context, onResult: ((Boolean, String?) -> Unit)? = null) {
        com.tesladroid.core.receiver.WifiAutoStartManager.userManuallyStopped = false
        _connectionState.value = ConnectionState.PREPARING

        // Sync all preferences to native engine storage
        AppPreferences(context).syncAllToTeslaMirror()

        // 1. Start LocalAddressVpn (establishes 100.99.9.9 interface for car) if not running
        try {
            val vpnClass = try {
                Class.forName("com.invmaster.teslamirror.LocalAddressVpn")
                "com.invmaster.teslamirror.LocalAddressVpn"
            } catch (_: Throwable) {
                "com.tesladroid.engine.LocalAddressVpn"
            }
            val vpnIntent = Intent().setClassName(context.packageName, vpnClass)
            context.startService(vpnIntent)
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "LocalAddressVpn start error: ${e.message}")
        }

        // 2. Bind UserService via ShizukuBridge before starting CaptureService
        try {
            val shizukuBridgeClass = try {
                Class.forName("com.invmaster.teslamirror.ShizukuBridge")
            } catch (_: Throwable) {
                null
            }

            if (shizukuBridgeClass != null) {
                val instanceField = shizukuBridgeClass.getField("a")
                val shizukuBridgeInstance = instanceField.get(null)
                val g4cClass = Class.forName("g4.c")
                val callback = java.lang.reflect.Proxy.newProxyInstance(
                    context.classLoader,
                    arrayOf(g4cClass)
                ) { _, method, args ->
                    if (method.name == "D") {
                        val userService = args?.get(0)
                        val note = args?.get(1) as? String
                        Log.i("CoreEngineBridge", "ShizukuBridge bind callback: userService=$userService, note=$note")
                        handleShizukuReady(context, userService, note, onResult)
                    }
                    null
                }

                val bindMethod = shizukuBridgeClass.getMethod("a", Context::class.java, g4cClass)
                bindMethod.invoke(shizukuBridgeInstance, context, callback)
            } else {
                ShizukuBridge.instance.bindUserService(context) { userService, note ->
                    Log.i("CoreEngineBridge", "Fallback ShizukuBridge bind callback: userService=$userService, note=$note")
                    handleShizukuReady(context, userService, note, onResult)
                }
            }
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "Error invoking ShizukuBridge.bindUserService", e)
            _connectionState.value = ConnectionState.ERROR
            android.widget.Toast.makeText(
                context,
                "Shizuku initialization error: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            onResult?.invoke(false, e.message)
        }
    }

    private fun handleShizukuReady(
        context: Context,
        userService: Any?,
        note: String?,
        onResult: ((Boolean, String?) -> Unit)?
    ) {
        Handler(Looper.getMainLooper()).post {
            if (userService != null) {
                AppPreferences(context).syncAllToTeslaMirror()
                startHotspot(context, userService)
                Thread {
                    try {
                        ShizukuBridge.execShell("pm grant com.tesladroid android.permission.WRITE_SECURE_SETTINGS; pm grant com.tesladroid android.permission.RECORD_AUDIO; setprop service.adb.tcp.port 5555")
                    } catch (_: Throwable) {}
                    com.tesladroid.core.crypto.CertificateManager.ensureCertificate(context)
                    Handler(Looper.getMainLooper()).post {
                        launchCaptureService(context)
                        _connectionState.value = ConnectionState.STREAMING
                        _currentFps.value = _streamQuality.value.targetFps
                        _currentBitrateMbps.value = _streamQuality.value.bitrateMbps.toFloat()
                        startTelemetryMonitor(context)
                        startBtWatcher(context)
                        refreshShortCode(context)
                        if (AppPreferences(context).wakeWordEnabled) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                notifyWakeWordChanged(context, true)
                            }, 1000L)
                        }
                        onResult?.invoke(true, null)
                    }
                }.start()
            } else {
                Log.e("CoreEngineBridge", "Failed to bind Shizuku user-service: $note")
                _connectionState.value = ConnectionState.ERROR
                android.widget.Toast.makeText(
                    context,
                    note ?: "Failed to start Shizuku service",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                onResult?.invoke(false, note)
            }
        }
    }

    private fun launchCaptureService(context: Context) {
        try {
            val token = getEffectiveSessionCode(context)
            syncTokenAcrossStorage(context, token)
            com.tesladroid.core.crypto.CertificateManager.ensureCertificate(context)
            val captureClass = try {
                Class.forName("com.invmaster.teslamirror.CaptureService")
                "com.invmaster.teslamirror.CaptureService"
            } catch (_: Throwable) {
                "com.tesladroid.engine.CaptureService"
            }
            val captureIntent = Intent().setClassName(context.packageName, captureClass).apply {
                putExtra("mode", "shizuku")
                putExtra("quality", _streamQuality.value.name)
                putExtra("audio_mode", _audioMode.value.name)
            }
            acquireStreamingWakeLock(context)
            // Keep the process foreground while the legacy native service initializes.
            // Android 13 kills that service if it does not call startForeground quickly.
            val foregroundIntent = Intent(context, TeslaStreamingService::class.java).apply {
                action = TeslaStreamingService.ACTION_START
            }
            context.startForegroundService(foregroundIntent)
            // The native service is legacy code and does not call startForeground on
            // Android 13. The wrapper above keeps this process foreground, so start
            // the native service normally and avoid Android's FGS startup timeout.
            context.startService(captureIntent)
            Log.i("CoreEngineBridge", "$captureClass started as foreground service (token: $token)")
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "CaptureService start error: ${e.message}")
        }
    }

    fun getEffectiveSessionCode(context: Context): String {
        val sp = context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
        val customShort = sp.getString("short_code", null)?.trim()
        val code = if (!customShort.isNullOrEmpty()) customShort else getSessionToken(context)
        syncTokenAcrossStorage(context, code)
        return code
    }

    fun getSessionToken(context: Context): String {
        try {
            val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
            val field = streamServerClass.getDeclaredField("b")
            field.isAccessible = true
            val b = field.get(null) as? String
            if (!b.isNullOrEmpty()) {
                syncTokenAcrossStorage(context, b.trim())
                return b.trim()
            }
        } catch (_: Throwable) {}

        val tmSp = context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)
        var token = tmSp.getString("session_token", null)?.trim() ?: ""

        if (token.isEmpty()) {
            val tdSp = context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
            token = tdSp.getString("session_token", null)?.trim() ?: ""
        }

        if (token.isEmpty()) {
            val alphabet = "abcdefghijkmnpqrstuvwxyz23456789"
            val sr = java.security.SecureRandom()
            val sb = StringBuilder(6)
            for (i in 0 until 6) {
                sb.append(alphabet[sr.nextInt(alphabet.length)])
            }
            token = sb.toString()
        }

        syncTokenAcrossStorage(context, token)
        return token
    }

    fun syncTokenAcrossStorage(context: Context, token: String) {
        if (token.isEmpty()) return
        try {
            context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)
                .edit().putString("session_token", token).apply()
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "Failed to sync token to teslamirror: ${e.message}")
        }
        try {
            context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
                .edit().putString("session_token", token).apply()
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "Failed to sync token to tesladroid_prefs: ${e.message}")
        }
        try {
            val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
            val field = streamServerClass.getDeclaredField("b")
            field.isAccessible = true
            field.set(null, token)
        } catch (_: Throwable) {}
        try {
            StreamServer.sessionToken = token
        } catch (_: Throwable) {}
    }

    fun getDeviceNodeId(context: Context): String {
        return com.tesladroid.core.crypto.CertificateManager.getNodeId(context)
    }

    fun getCertHost(context: Context): String {
        var certName: String? = null
        try {
            certName = CertStore.getCertName(context)?.trim()
        } catch (_: Throwable) {}

        if (certName.isNullOrEmpty()) {
            try {
                val spLicense = context.getSharedPreferences("license", Context.MODE_PRIVATE)
                certName = spLicense.getString("cert_name", null)?.trim()
            } catch (_: Throwable) {}
        }

        if (certName.isNullOrEmpty()) {
            val sp = context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
            certName = sp.getString("cert_name", null)?.trim()
        }

        if (certName.isNullOrEmpty() || certName == "car.tesla-droid.app" || !certName.contains(".")) {
            certName = com.tesladroid.core.crypto.CertificateManager.getNodeHostname(context)
        }
        return certName.trim()
    }

    fun registerSessionOnCloud(context: Context) {
        val code = getEffectiveSessionCode(context)
        val host = getCertHost(context)
        Thread {
            val endpoints = listOf(
                "https://car.tesladroid.workers.dev/register",
                "https://redirect.tesladroid.workers.dev/register"
            )
            val payload = """{"code":"$code","host":"$host"}"""
            for (endpoint in endpoints) {
                try {
                    val url = java.net.URL(endpoint)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("User-Agent", "TeslaDroid/1.0")
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val resp = conn.responseCode
                    if (resp in 200..299) {
                        Log.i("CoreEngineBridge", "Session registered on cloud ($endpoint): $code -> $host ($resp)")
                        break
                    }
                } catch (e: Throwable) {
                    Log.w("CoreEngineBridge", "Cloud registration failed on $endpoint: ${e.message}")
                }
            }
        }.start()
    }

    fun initUrl(context: Context) {
        com.tesladroid.core.crypto.CertificateManager.ensureCertificateAsync(context) { success ->
            if (success) {
                Handler(Looper.getMainLooper()).post {
                    _browserUrl.value = getTeslaBrowserUrl(context)
                    _shortUrl.value = getShortBrowserUrl(context)
                    refreshShortCode(context)
                }
            }
        }
        _browserUrl.value = getTeslaBrowserUrl(context)
        _shortUrl.value = getShortBrowserUrl(context)
        refreshShortCode(context)
    }

    fun getShortBrowserUrl(context: Context): String {
        val code = getEffectiveSessionCode(context)
        return "car.tesla-droid.app/$code"
    }

    fun refreshShortCode(context: Context) {
        val code = getEffectiveSessionCode(context)
        val updatedShortUrl = "car.tesla-droid.app/$code"
        _shortUrl.value = updatedShortUrl
        _browserUrl.value = getTeslaBrowserUrl(context)
        Log.i("CoreEngineBridge", "Short URL updated: $updatedShortUrl")
        registerSessionOnCloud(context)
    }

    fun getTeslaBrowserUrl(context: Context): String {
        val certName = getCertHost(context)
        val token = getEffectiveSessionCode(context)
        return "https://$certName:8443/$token/"
    }

    fun isHotspotActive(context: Context): Boolean {
        return com.tesladroid.core.util.HotspotHelper.isHotspotActive(context)
    }

    fun startHotspot(context: Context, service: Any? = null) {
        // Hotspot is started by default when streaming session begins
        Thread {
            try {
                Log.i("CoreEngineBridge", "Starting Wi-Fi hotspot for vehicle streaming...")
                context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)
                    .edit().putBoolean("tether_by_us", true).apply()
                context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("tether_by_us", true).apply()

                SessionStop.shouldStopTether = true
                try {
                    val stopClass = Class.forName("com.invmaster.teslamirror.SessionStop")
                    stopClass.getField("b").set(null, true)
                } catch (_: Throwable) {}

                val started = com.tesladroid.core.util.HotspotHelper.startHotspot(context, service)
                Log.i("CoreEngineBridge", "Hotspot startHotspot result: $started")
            } catch (e: Throwable) {
                Log.e("CoreEngineBridge", "startHotspot error", e)
            }
        }.start()
    }

    fun stopHotspot(context: Context, service: Any? = null) {
        val prefs = AppPreferences(context)
        if (!prefs.autoHotspot) {
            Log.i("CoreEngineBridge", "Auto-stop hotspot is disabled by user preference (leaving hotspot running).")
            return
        }

        Thread {
            try {
                Log.i("CoreEngineBridge", "Stopping Wi-Fi hotspot on session exit...")
                SessionStop.shouldStopTether = true
                try {
                    val stopClass = Class.forName("com.invmaster.teslamirror.SessionStop")
                    stopClass.getField("b").set(null, true)
                } catch (_: Throwable) {}

                val stopped = com.tesladroid.core.util.HotspotHelper.stopHotspot(context, service)
                Log.i("CoreEngineBridge", "stopHotspot result: $stopped")

                context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)
                    .edit().putBoolean("tether_by_us", false).apply()
                context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("tether_by_us", false).apply()
            } catch (e: Throwable) {
                Log.e("CoreEngineBridge", "stopHotspot error", e)
            }
        }.start()
    }

    fun stopStreaming(context: Context) {
        com.tesladroid.core.receiver.WifiAutoStartManager.userManuallyStopped = true
        monitorJob?.cancel()
        monitorJob = null
        stopAppMigrationWatcher()
        stopBtWatcher(context)
        releaseStreamingWakeLock()

        // 1. Clean up background apps and pause media
        forceStopNonWhitelistedApps(context)

        // 2. Stop Hotspot if user preference autoHotspot is enabled
        stopHotspot(context)

        // 3. Stop CaptureService
        try {
            val captureClass = try {
                Class.forName("com.invmaster.teslamirror.CaptureService")
                "com.invmaster.teslamirror.CaptureService"
            } catch (_: Throwable) {
                "com.tesladroid.engine.CaptureService"
            }
            val captureIntent = Intent().setClassName(context.packageName, captureClass)
            context.stopService(captureIntent)
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "CaptureService stop error: ${e.message}")
        }

        // 4. Stop LocalAddressVpn
        try {
            val vpnClass = try {
                Class.forName("com.invmaster.teslamirror.LocalAddressVpn")
                "com.invmaster.teslamirror.LocalAddressVpn"
            } catch (_: Throwable) {
                "com.tesladroid.engine.LocalAddressVpn"
            }
            val vpnIntent = Intent().setClassName(context.packageName, vpnClass).apply {
                action = if (vpnClass.startsWith("com.invmaster")) {
                    "com.invmaster.teslamirror.VPN_STOP"
                } else {
                    "com.tesladroid.engine.VPN_STOP"
                }
            }
            context.startService(vpnIntent)
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "LocalAddressVpn stop error: ${e.message}")
        }

        // 5. Stop UI Foreground Service
        try {
            val serviceIntent = Intent(context, TeslaStreamingService::class.java).apply {
                action = TeslaStreamingService.ACTION_STOP
            }
            context.stopService(serviceIntent)
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "TeslaStreamingService stop error: ${e.message}")
        }

        try {
            com.tesladroid.core.receiver.WifiAutoStartManager.cancelDisconnectTimer()
        } catch (_: Throwable) {}

        _connectionState.value = ConnectionState.IDLE
        _connectedClientsCount.value = 0
        _currentFps.value = 0
        _currentBitrateMbps.value = 0.0f
        _streamingDurationSeconds.value = 0L
    }

    private fun forceStopNonWhitelistedApps(context: Context) {
        Thread {
            try {
                // Pause active media playback (YouTube, Spotify, etc.)
                try {
                    ShizukuBridge.execShell("cmd media_session dispatch pause")
                    ShizukuBridge.execShell("input keyevent 127") // KEYCODE_MEDIA_PAUSE
                } catch (_: Throwable) {}

                // Do not invoke the legacy global SessionStop cleanup here.  It can tear
                // down the Shizuku user-service together with the streaming session.
                // Capture/VPN cleanup is performed explicitly by stopStreaming().

                val prefs = AppPreferences(context)
                val keepAlive = prefs.keepAliveApps + setOf(
                    context.packageName,
                    "android",
                    "com.android.systemui",
                    "moe.shizuku.privileged.api",
                    "rikka.shizuku",
                    "com.google.android.inputmethod.latin"
                )

                // If YouTube apps are not whitelisted, stop them immediately so audio doesn't keep playing
                if (!keepAlive.contains("com.google.android.youtube")) {
                    ShizukuBridge.execShell("am force-stop com.google.android.youtube")
                }
                if (!keepAlive.contains("com.google.android.apps.youtube.music")) {
                    ShizukuBridge.execShell("am force-stop com.google.android.apps.youtube.music")
                }

                // Inspect activities running on virtual display and kill non-whitelisted apps
                val dump = ShizukuBridge.execShell("dumpsys activity activities")
                val displaySections = dump.split(Regex("Display #(\\d+)"))
                if (displaySections.size > 1) {
                    for (i in 1 until displaySections.size) {
                        val section = displaySections[i]
                        val pkgMatches = Regex("\\b(?:u\\d+\\s+|packageName=)([a-zA-Z][a-zA-Z0-9_.]*\\.[a-zA-Z0-9_.]+)").findAll(section)
                        for (m in pkgMatches) {
                            val pkg = m.groupValues[1]
                            if (!keepAlive.contains(pkg) && !pkg.startsWith("android") && !pkg.startsWith("com.android.internal")) {
                                Log.i("CoreEngineBridge", "Keep-running cleanup: stopping $pkg")
                                ShizukuBridge.execShell("am force-stop $pkg")
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w("CoreEngineBridge", "forceStopNonWhitelistedApps error: ${t.message}")
            }
        }.start()
    }

    private fun acquireStreamingWakeLock(context: Context) {
        try {
            if (streamingWakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                streamingWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TeslaDroid:streamingActive")
                streamingWakeLock?.setReferenceCounted(false)
            }
            streamingWakeLock?.acquire(8 * 3600 * 1000L) // Keep active during streaming
            Log.i("CoreEngineBridge", "Streaming WakeLock acquired (screen lock streaming enabled)")
        } catch (t: Throwable) {
            Log.w("CoreEngineBridge", "Failed to acquire streaming wake lock: ${t.message}")
        }
    }

    private fun releaseStreamingWakeLock() {
        try {
            if (streamingWakeLock?.isHeld == true) {
                streamingWakeLock?.release()
                Log.i("CoreEngineBridge", "Streaming WakeLock released")
            }
        } catch (_: Throwable) {}
    }

    private fun acquireWakeLock(context: Context, timeoutMs: Long = 35000L) {
        try {
            if (wakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TeslaDroid:btDisconnectStop")
                wakeLock?.setReferenceCounted(false)
            }
            wakeLock?.acquire(timeoutMs)
            Log.i("CoreEngineBridge", "WakeLock acquired for $timeoutMs ms")
        } catch (t: Throwable) {
            Log.w("CoreEngineBridge", "Failed to acquire wake lock: ${t.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i("CoreEngineBridge", "WakeLock released")
            }
        } catch (_: Throwable) {}
    }

    private fun startBtWatcher(context: Context) {
        stopBtWatcher(context)
        val prefs = AppPreferences(context)
        val carBt = prefs.carBtAddr.trim()
        if (carBt.isBlank()) {
            Log.i("CoreEngineBridge", "No car Bluetooth address configured, skipping BT disconnect watcher.")
            return
        }

        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val action = intent.action ?: return
                    val device = @Suppress("DEPRECATION") intent.getParcelableExtra<android.bluetooth.BluetoothDevice>(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                    val deviceAddr = try { device?.address ?: "" } catch (_: SecurityException) { "" }

                    if (action == android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                        (action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED &&
                         intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1) == android.bluetooth.BluetoothAdapter.STATE_OFF)) {

                        val isCarDevice = action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED ||
                                          deviceAddr.equals(carBt, ignoreCase = true)

                        if (isCarDevice && _connectionState.value != ConnectionState.IDLE) {
                            if (btDisconnectJob == null) {
                                Log.i("CoreEngineBridge", "Car Bluetooth ($carBt) disconnected! Waiting 30s before auto-stop...")
                                acquireWakeLock(context, 35000L)
                                btDisconnectJob = scope.launch {
                                    delay(30_000L)
                                    Log.i("CoreEngineBridge", "30 seconds without car Bluetooth elapsed. Auto-stopping streaming session!")
                                    withContext(Dispatchers.Main) {
                                        stopStreaming(context)
                                        dismissSystemDim()
                                    }
                                    releaseWakeLock()
                                    btDisconnectJob = null
                                }
                            }
                        }
                    } else if (action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED) {
                        if (deviceAddr.equals(carBt, ignoreCase = true) && btDisconnectJob != null) {
                            Log.i("CoreEngineBridge", "Car Bluetooth ($carBt) reconnected! Cancelling auto-stop.")
                            btDisconnectJob?.cancel()
                            btDisconnectJob = null
                            releaseWakeLock()
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            btReceiver = receiver
            Log.i("CoreEngineBridge", "Bluetooth disconnect watcher registered for car: $carBt")
        } catch (t: Throwable) {
            Log.w("CoreEngineBridge", "Failed to register BT watcher: ${t.message}")
        }
    }

    private fun stopBtWatcher(context: Context) {
        btDisconnectJob?.cancel()
        btDisconnectJob = null
        releaseWakeLock()
        if (btReceiver != null) {
            try {
                context.unregisterReceiver(btReceiver)
            } catch (_: Throwable) {}
            btReceiver = null
        }
    }

    private fun startTelemetryMonitor(context: Context) {
        monitorJob?.cancel()
        startAppMigrationWatcher(context)
        monitorJob = scope.launch(Dispatchers.IO) {
            var duration = 0L
            var hadClientConnected = false
            var zeroClientsSinceMs = 0L
            var hasAutoLaunchedApp = false

            while (isActive) {
                delay(1000L)
                duration++
                _streamingDurationSeconds.value = duration

                val currentUrl = getTeslaBrowserUrl(context)
                if (_browserUrl.value != currentUrl) {
                    _browserUrl.value = currentUrl
                }

                val currentShortUrl = getShortBrowserUrl(context)
                if (_shortUrl.value != currentShortUrl) {
                    _shortUrl.value = currentShortUrl
                }

                // Poll StreamServer status
                try {
                    val isRunning: Boolean = try {
                        val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
                        val isRunningMethod = try {
                            streamServerClass.getMethod("o")
                        } catch (_: Throwable) {
                            streamServerClass.getMethod("n")
                        }
                        isRunningMethod.invoke(null) as? Boolean ?: false
                    } catch (_: Throwable) {
                        StreamServer.isRunning
                    }

                    val clientCount: Int = try {
                        val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
                        val clientsField = streamServerClass.getField("g")
                        (clientsField.get(null) as? List<*>)?.size ?: 0
                    } catch (_: Throwable) {
                        StreamServer.connectedClients?.size ?: 0
                    }

                    try {
                        val fps = try {
                            val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
                            val fpsField = streamServerClass.getField("m")
                            fpsField.getInt(null)
                        } catch (_: Throwable) {
                            StreamServer.currentFps
                        }
                        if (fps > 0) {
                            _currentFps.value = fps
                        }
                    } catch (_: Throwable) {}

                    try {
                        val mbps = try {
                            val autoBitrateClass = Class.forName("com.invmaster.teslamirror.AutoBitrate")
                            val bField = autoBitrateClass.getField("b")
                            bField.getInt(null)
                        } catch (_: Throwable) {
                            AutoBitrate.currentBitrateMbps
                        }
                        if (mbps > 0) {
                            _currentBitrateMbps.value = mbps.toFloat()
                        }
                    } catch (_: Throwable) {}

                    if (isRunning && clientCount > 0) {
                        hadClientConnected = true
                        zeroClientsSinceMs = 0L
                        _connectionState.value = ConnectionState.CAR_CONNECTED
                        _connectedClientsCount.value = clientCount

                        if (!hasAutoLaunchedApp) {
                            hasAutoLaunchedApp = true
                            val prefs = AppPreferences(context)
                            val targetPkg = prefs.autoLaunchPackage.trim()
                            if (targetPkg.isNotEmpty()) {
                                Thread {
                                    try {
                                        Thread.sleep(1200)
                                        var displayId = -1
                                        for (attempt in 0 until 20) {
                                            displayId = getActiveVirtualDisplayId()
                                            if (displayId > 0) break
                                            Thread.sleep(500)
                                        }
                                        Log.i("CoreEngineBridge", "Auto-launch found virtual displayId: $displayId")

                                        // Wake screen and dismiss keyguard so app is not blocked by lock screen
                                        try {
                                            ShizukuBridge.execShell("input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard")
                                        } catch (_: Throwable) {}

                                        val cleanPkg = if (targetPkg.contains("/")) targetPkg.substringBefore("/") else targetPkg
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(cleanPkg)
                                        val resolvedComponent = launchIntent?.component?.flattenToShortString() ?: targetPkg

                                        val cmd = if (displayId > 0) {
                                            if (resolvedComponent.contains("/")) "am start --display $displayId -n '$resolvedComponent'"
                                            else "am start --display $displayId -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p '$cleanPkg'"
                                        } else {
                                            if (resolvedComponent.contains("/")) "am start -n '$resolvedComponent'"
                                            else "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p '$cleanPkg'"
                                        }
                                        Log.i("CoreEngineBridge", "Auto-launching app on vehicle connect: $cmd")
                                        val res = ShizukuBridge.execShell(cmd)
                                        Log.i("CoreEngineBridge", "Auto-launch result: $res")
                                        if (res.contains("Error", ignoreCase = true) || res.contains("unable to resolve", ignoreCase = true)) {
                                            val fallbackCmd = if (displayId > 0) "monkey --display $displayId --pure-exec -p $cleanPkg -c android.intent.category.LAUNCHER 1"
                                            else "monkey --pure-exec -p $cleanPkg -c android.intent.category.LAUNCHER 1"
                                            Log.i("CoreEngineBridge", "Auto-launch fallback: $fallbackCmd")
                                            ShizukuBridge.execShell(fallbackCmd)
                                        }
                                    } catch (e: Throwable) {
                                        Log.w("CoreEngineBridge", "Auto-launch error: ${e.message}")
                                    }
                                }.start()
                            }
                        }
                    } else if (isRunning) {
                        _connectionState.value = ConnectionState.STREAMING
                        _connectedClientsCount.value = 0

                        // Auto-stop grace countdown when vehicle browser disconnects or leaves page
                        if (hadClientConnected) {
                            if (zeroClientsSinceMs == 0L) {
                                zeroClientsSinceMs = SystemClock.elapsedRealtime()
                            }
                            val elapsedSec = (SystemClock.elapsedRealtime() - zeroClientsSinceMs) / 1000L
                            val userAutoStopSec = AppPreferences(context).autoStopSeconds
                            val autoStopThreshold = if (userAutoStopSec > 0) userAutoStopSec else 30

                            if (elapsedSec >= autoStopThreshold) {
                                Log.i("CoreEngineBridge", "Auto-stop: Vehicle browser was disconnected for $elapsedSec s (threshold: $autoStopThreshold s). Stopping stream!")
                                withContext(Dispatchers.Main) {
                                    stopStreaming(context)
                                    dismissSystemDim()
                                }
                                break
                            }
                        }
                    } else {
                        if (_connectionState.value != ConnectionState.IDLE) {
                            Log.i("CoreEngineBridge", "StreamServer stopped. Setting state to IDLE.")
                            withContext(Dispatchers.Main) {
                                stopStreaming(context)
                                dismissSystemDim()
                            }
                        }
                    }
                } catch (e: Throwable) {
                    // Running in pure stub mode if backend components not yet started
                }
            }
        }
    }

    fun getActiveVirtualDisplayId(): Int {
        try {
            val captureClass = Class.forName("com.invmaster.teslamirror.CaptureService")
            for (fieldName in listOf("E1", "e1", "l1", "w1", "displayId", "mDisplayId")) {
                try {
                    val f = captureClass.getField(fieldName)
                    val id = f.getInt(null)
                    if (id > 0) return id
                } catch (_: Throwable) {}
            }
            try {
                val e1 = captureClass.getField("e1").get(null)
                if (e1 != null) {
                    val eField = captureClass.getField("e")
                    val vd = eField.get(e1) as? android.hardware.display.VirtualDisplay
                    val dId = vd?.display?.displayId ?: -1
                    if (dId > 0) return dId
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}

        // Fallback: query dumpsys display for any non-default display ID
        try {
            val dump = ShizukuBridge.execShell("dumpsys display")
            val matches = Regex("mDisplayId=([1-9]\\d*)").findAll(dump)
            for (m in matches) {
                val id = m.groupValues[1].toIntOrNull() ?: 0
                if (id > 0) return id
            }
        } catch (_: Throwable) {}
        return -1
    }

    private fun startAppMigrationWatcher(context: Context) {
        stopAppMigrationWatcher()
        appWatcherJob = scope.launch(Dispatchers.IO) {
            var lastMovedTaskId = -1
            var lastMovedPkg = ""
            var lastMovedTime = 0L

            while (isActive) {
                delay(500L)
                if (_connectionState.value == ConnectionState.IDLE) break

                val vdId = getActiveVirtualDisplayId()
                if (vdId <= 0) continue

                try {
                    // Fast query (approx 15-20ms) for the top resumed activity on phone display 0
                    val line = ShizukuBridge.execShell("dumpsys activity activities | awk '/Display #0/{flag=1} /Display #[1-9]/{flag=0} flag && /topResumedActivity/{print; exit}'")
                    if (!line.contains("topResumedActivity")) continue

                    val pkgMatch = Regex("u\\d+\\s+([a-zA-Z0-9_.]+)/").find(line)
                    val taskMatch = Regex("t(\\d+)").find(line)
                    val pkg = pkgMatch?.groupValues?.get(1)
                    val taskId = taskMatch?.groupValues?.get(1)?.toIntOrNull()

                    if (pkg != null && taskId != null && taskId > 0) {
                        // Ignore system UI, TeslaDroid itself, launchers, and Gemini/Google overlay
                        if (pkg == context.packageName ||
                            pkg == "com.google.android.googlequicksearchbox" ||
                            pkg == "com.android.systemui" ||
                            pkg.contains("launcher", ignoreCase = true) ||
                            pkg.contains("home", ignoreCase = true)
                        ) {
                            continue
                        }

                        // Only migrate apps present in menu_apps / favorites whitelist
                        val menuApps = AppPreferences(context).menuApps
                        if (!menuApps.contains(pkg)) {
                            // Non-whitelisted apps remain running on phone in background
                            continue
                        }

                        // Debounce if task was recently migrated
                        if (taskId == lastMovedTaskId && (SystemClock.elapsedRealtime() - lastMovedTime < 4000L)) {
                            continue
                        }

                        lastMovedTaskId = taskId
                        lastMovedPkg = pkg
                        lastMovedTime = SystemClock.elapsedRealtime()

                        Log.i("CoreEngineBridge", "Gemini/Assistant opened menu app $pkg (task #$taskId) on phone. Migrating to vehicle display #$vdId...")

                        val moveRes = ShizukuBridge.execShell("am display move-stack $taskId $vdId")
                        Log.i("CoreEngineBridge", "am display move-stack result: $moveRes")
                        ShizukuBridge.execShell("am task focus $taskId")

                        // Fallback: if move-stack returned an error or task didn't respond, launch on display
                        if (moveRes.contains("Error", ignoreCase = true) || moveRes.contains("Exception", ignoreCase = true)) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                            val resolvedComponent = launchIntent?.component?.flattenToShortString()
                            if (resolvedComponent != null) {
                                ShizukuBridge.execShell("am start --display $vdId -n '$resolvedComponent'")
                            } else {
                                ShizukuBridge.execShell("am start --display $vdId -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p '$pkg'")
                            }
                        }

                        // If phone screen dimming is active, restore dimming overlay on phone display 0
                        val prefs = AppPreferences(context)
                        if (prefs.lockDim || isDimActive) {
                            triggerSystemDim("assistant_launch")
                        }
                        resumeKeepPlayingApps(context, pkg)
                    }
                } catch (t: Throwable) {
                    Log.w("CoreEngineBridge", "App migration watcher tick error: ${t.message}")
                }
            }
        }
    }

    private fun stopAppMigrationWatcher() {
        appWatcherJob?.cancel()
        appWatcherJob = null
    }

    fun resumeKeepPlayingApps(context: Context, excludePkg: String = "") {
        val keepApps = AppPreferences(context).keepPlayingApps
        if (keepApps.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            delay(750L)
            try {
                val targets = keepApps.filter { it != excludePkg }
                if (targets.isEmpty()) return@launch

                val userService = ShizukuBridge.currentUserService
                if (userService != null) {
                    val m = userService.javaClass.getMethod("resumePlayback", String::class.java)
                    val pkgs = targets.joinToString(",")
                    m.invoke(userService, pkgs)
                    Log.i("CoreEngineBridge", "Resumed playback via IUserService for: $pkgs")
                }

                for (targetPkg in targets) {
                    ShizukuBridge.execShell("cmd media_session dispatch play --package $targetPkg")
                }
            } catch (t: Throwable) {
                Log.w("CoreEngineBridge", "resumeKeepPlayingApps error: ${t.message}")
            }
        }
    }


    fun notifyCarConnected(connected: Boolean) {
        if (connected && _connectionState.value == ConnectionState.STREAMING) {
            _connectionState.value = ConnectionState.CAR_CONNECTED
            _connectedClientsCount.value = 1
        } else if (!connected && _connectionState.value == ConnectionState.CAR_CONNECTED) {
            _connectionState.value = ConnectionState.STREAMING
            _connectedClientsCount.value = 0
        }
    }

    fun notifyDisplayDpiChanged(context: Context) {
        try {
            val captureService = getNativeCaptureService()
            if (captureService != null) {
                val cls = captureService.javaClass
                var modeStr: String? = null
                for (fName in listOf("N", "K", "mMode", "mode")) {
                    try {
                        val f = cls.getField(fName)
                        val v = f.get(captureService)
                        if (v is String) {
                            modeStr = v
                            break
                        }
                    } catch (_: Throwable) {}
                }
                Thread {
                    try {
                        val iMethod = cls.getMethod("i", String::class.java)
                        iMethod.invoke(captureService, modeStr)
                        Log.i("CoreEngineBridge", "Virtual display resized immediately for new DPI via i($modeStr)")
                    } catch (t: Throwable) {
                        Log.w("CoreEngineBridge", "Failed to update display mode via i(): ${t.message}")
                    }
                }.start()
            }
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "notifyDisplayDpiChanged error: ${e.message}")
        }
    }

    fun notifyOdometerChanged(context: Context, enabled: Boolean) {
        try {
            val odoClass = Class.forName("com.invmaster.teslamirror.Odometer")
            val instanceField = odoClass.getField("f1683a")
            val odo = instanceField.get(null)
            if (odo != null) {
                val kMethod = odoClass.getMethod("k", Context::class.java, String::class.java, Boolean::class.javaPrimitiveType)
                kMethod.invoke(odo, context, "settings", enabled)
                Log.i("CoreEngineBridge", "Odometer state pushed immediately: enabled=$enabled")
            }
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "notifyOdometerChanged error: ${e.message}")
        }
    }

    fun notifyCarGpsNavChanged(context: Context, enabled: Boolean) {
        if (!enabled) {
            try {
                val mockGpsClass = Class.forName("com.invmaster.teslamirror.MockGps")
                val instanceField = mockGpsClass.getField("f1627a")
                val mockGps = instanceField.get(null)
                if (mockGps != null) {
                    val sMethod = mockGpsClass.getMethod("s", Context::class.java, String::class.java)
                    sMethod.invoke(mockGps, context, "settings")
                    Log.i("CoreEngineBridge", "MockGps stopped immediately as GPS Shield was disabled")
                }
            } catch (e: Throwable) {
                Log.w("CoreEngineBridge", "Failed to stop MockGps: ${e.message}")
            }
        }
        notifyPhoneConfigChanged(context)
    }

    fun notifyPhoneConfigChanged(context: Context) {
        try {
            val captureService = getNativeCaptureService()
            if (captureService != null) {
                val a0Method = captureService.javaClass.getMethod("a0")
                a0Method.invoke(captureService)
                Log.i("CoreEngineBridge", "Phone config broadcasted to Tesla browser via CaptureService.a0()")
            }
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "notifyPhoneConfigChanged error: ${e.message}")
        }
    }

    fun notifyWakeWordChanged(context: Context, enabled: Boolean) {
        try {
            val wwCls = try {
                Class.forName("com.invmaster.teslamirror.WakeWord")
            } catch (_: Throwable) {
                Class.forName("com.tesladroid.engine.WakeWord")
            }
            val threshold = AppPreferences(context).wakeThreshold
            try {
                val bField = wwCls.getField("b")
                bField.setFloat(null, threshold / 100.0f)
                Log.i("CoreEngineBridge", "Updated WakeWord threshold to ${threshold / 100.0f}")
            } catch (_: Throwable) {}

            if (enabled) {
                val service = getNativeCaptureService()
                if (service != null) {
                    try {
                        val csCls = Class.forName("com.invmaster.teslamirror.CaptureService")
                        val eMethod = wwCls.getDeclaredMethod("e", csCls).apply { isAccessible = true }
                        eMethod.invoke(null, service)
                        Log.i("CoreEngineBridge", "WakeWord started successfully via native detector")
                    } catch (e: Throwable) {
                        Log.w("CoreEngineBridge", "WakeWord start error: ${e.message}")
                    }
                }
            } else {
                try {
                    val gMethod = wwCls.getDeclaredMethod("g", String::class.java).apply { isAccessible = true }
                    gMethod.invoke(null, "settings")
                    Log.i("CoreEngineBridge", "WakeWord stopped via native detector")
                } catch (e: Throwable) {
                    Log.w("CoreEngineBridge", "WakeWord stop error: ${e.message}")
                }
            }
            notifyPhoneConfigChanged(context)
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "notifyWakeWordChanged error: ${e.message}")
        }
    }

    fun notifyBitrateChanged(mbps: Int) {
        updateBitrate(mbps)
        try {
            AutoBitrate.currentBitrateMbps = mbps
        } catch (_: Throwable) {}
        try {
            val autoBitrateClass = Class.forName("com.invmaster.teslamirror.AutoBitrate")
            val bField = autoBitrateClass.getField("b")
            bField.setInt(null, mbps)
        } catch (_: Throwable) {}

        try {
            val codec = try {
                val captureService = getNativeCaptureService()
                if (captureService != null) {
                    val fField = captureService.javaClass.getField("f")
                    fField.get(captureService) as? android.media.MediaCodec
                } else null
            } catch (_: Throwable) {
                CaptureService.instance?.activeCodec
            }

            if (codec != null) {
                val bundle = android.os.Bundle().apply {
                    putInt("video-bitrate", mbps * 1_000_000)
                }
                codec.setParameters(bundle)
                Log.i("CoreEngineBridge", "MediaCodec live bitrate set to $mbps Mbps")
            }
        } catch (e: Throwable) {
            Log.w("CoreEngineBridge", "Live bitrate update: ${e.message}")
        }
    }

    fun notifyAudioModeChanged(context: Context) {
        try {
            try {
                val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
                val jMethod = streamServerClass.getMethod("j", Context::class.java)
                jMethod.invoke(null, context)
            } catch (_: Throwable) {
                StreamServer.updateAudioMode(context)
            }
            Log.i("CoreEngineBridge", "Audio mode updated via StreamServer.j(context)")
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "notifyAudioModeChanged error: ${e.message}")
        }
    }

    fun notifyVideoDelayChanged(delayMs: Int) {
        try {
            try {
                val streamServerClass = Class.forName("com.invmaster.teslamirror.StreamServer")
                val yField = streamServerClass.getField("y")
                yField.setInt(null, delayMs)
                val iMethod = streamServerClass.getMethod("i")
                iMethod.invoke(null)
            } catch (_: Throwable) {
                StreamServer.videoDelayMs = delayMs
                StreamServer.applyVideoDelay()
            }
            Log.i("CoreEngineBridge", "Video delay updated to ${delayMs}ms via StreamServer")
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "notifyVideoDelayChanged error: ${e.message}")
        }
    }

    fun triggerSystemDim(reason: String = "button"): Boolean {
        Handler(Looper.getMainLooper()).post {
            try {
                showDimOverlay()
            } catch (t: Throwable) {
                Log.w("CoreEngineBridge", "Failed to show dim overlay: ${t.message}")
            }
        }
        return try {
            val res = SystemDim.dimScreen(reason)
            Log.i("CoreEngineBridge", "SystemDim dimScreen($reason) returned $res")
            res
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "triggerSystemDim error: ${e.message}")
            false
        }
    }

    fun dismissSystemDim() {
        Handler(Looper.getMainLooper()).post {
            try {
                hideDimOverlay()
            } catch (t: Throwable) {
                Log.w("CoreEngineBridge", "Failed to hide dim overlay: ${t.message}")
            }
        }
        try {
            SystemDim.restoreScreen()
            Log.i("CoreEngineBridge", "SystemDim restoreScreen() called to dismiss overlay")
        } catch (e: Throwable) {
            Log.e("CoreEngineBridge", "dismissSystemDim error: ${e.message}")
        }
    }

    private fun showDimOverlay() {
        val app = com.tesladroid.TeslaDroidApp.instance
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        if (isDimActive && dimView != null) return

        val view = View(app).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = false
            @Suppress("DEPRECATION")
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            setOnTouchListener { _, _ ->
                dismissSystemDim()
                true
            }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_FULLSCREEN
                    or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.OPAQUE
        ).apply {
            screenBrightness = 0.0f
            alpha = 1.0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            wm.addView(view, params)
            dimView = view
            isDimActive = true

            try {
                com.tesladroid.engine.ShizukuBridge.execShell("settings put global hide_gesture_line 1")
            } catch (_: Throwable) {}

            view.post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.windowInsetsController?.let { controller ->
                        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }

            // Also hide navigation bar on native SystemDim.d if present
            try {
                val nativeDimCls = Class.forName("com.invmaster.teslamirror.SystemDim")
                val nativeView = nativeDimCls.getField("d").get(null) as? View
                nativeView?.apply {
                    @Suppress("DEPRECATION")
                    systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                }
            } catch (_: Throwable) {}

            Log.i("CoreEngineBridge", "Fullscreen Dim overlay attached with completely hidden navigation bar")
        } catch (t: Throwable) {
            Log.w("CoreEngineBridge", "Failed to add dim overlay view: ${t.message}")
        }
    }

    private fun hideDimOverlay() {
        val app = com.tesladroid.TeslaDroidApp.instance
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        dimView?.let {
            try {
                wm.removeView(it)
            } catch (_: Throwable) {}
        }
        dimView = null
        isDimActive = false
        Log.i("CoreEngineBridge", "Fullscreen Dim overlay removed")
    }

    companion object {
        val instance: CoreEngineBridge by lazy { CoreEngineBridge() }
    }
}
