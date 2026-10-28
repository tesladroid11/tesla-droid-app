package com.tesladroid.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tesladroid.TeslaDroidApp
import com.tesladroid.core.engine.CoreEngineBridge
import com.tesladroid.core.model.AudioMode
import com.tesladroid.core.model.AudioSettingMode
import com.tesladroid.core.model.ConnectionState
import com.tesladroid.core.model.VideoBitrate
import com.tesladroid.core.receiver.WifiAutoStartManager
import com.tesladroid.core.util.AppPreferences
import com.tesladroid.core.util.SystemSettingsHelper
import com.tesladroid.engine.ShizukuBridge
import com.tesladroid.ui.dashboard.components.InstalledAppItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val bridge = CoreEngineBridge.instance
    private val prefs by lazy { AppPreferences(TeslaDroidApp.instance) }

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            autoStart = prefs.autoStart,
            selectedVideoBitrate = VideoBitrate.values().find { it.mbps == prefs.videoBitrateMbps } ?: VideoBitrate.MBPS_8,
            selectedAudioSettingMode = when (prefs.audioMode) {
                "WEB_OPUS" -> AudioSettingMode.WEB_OPUS
                "WEB_PCM" -> AudioSettingMode.WEB_PCM
                "OFF" -> AudioSettingMode.OFF
                else -> AudioSettingMode.WEB_PCM
            },
            autoLaunchPackage = prefs.autoLaunchPackage,
            selectedMenuApps = prefs.menuApps,
            keepAliveApps = prefs.keepAliveApps,
            keepPlayingApps = prefs.keepPlayingApps,
            autoStopSeconds = prefs.autoStopSeconds,
            autoDimSeconds = prefs.autoDimSeconds,
            menuScale = prefs.menuScale,
            stripLeft = prefs.stripLeft,
            pinRotation = prefs.pinRotation,
            displayDecorations = prefs.displayDecorations,
            hideNavBar = prefs.hideNavBar,
            odometer = prefs.odometer,
            carGpsNav = prefs.carGpsNav,
            displayDpi = prefs.displayDpi,
            fpsSetting = prefs.fps,
            audioPcm = prefs.audioPcm,
            videoDelayMs = prefs.videoDelayMs,
            hotspot5Ghz = prefs.hotspot5Ghz,
            autoHotspot = prefs.autoHotspot,
            inCarAssistantButtonEnabled = prefs.inCarAssistantButtonEnabled,
            googleAssistant = prefs.googleAssistant,
            voiceCues = prefs.voiceCues,
            wakeWordEnabled = prefs.wakeWordEnabled,
            wakeThreshold = prefs.wakeThreshold,
            phoneActions = prefs.phoneActions,
            glowSize = prefs.glowSize,
            geminiApiKey = prefs.geminiApiKey,
            geminiModel = prefs.geminiModel,
            geminiVoice = prefs.geminiVoice,
            hudShown = prefs.hudShown,
            carBtAddr = prefs.carBtAddr,
            carBtName = prefs.carBtName,
            lockDim = prefs.lockDim,
            powerReveal = prefs.powerReveal,
            notifPopup = prefs.notifPopup,
            notifText = prefs.notifText,
            notifOngoing = prefs.notifOngoing,
            muteNotif = prefs.muteNotif,
            shortCode = prefs.shortCode,
            wifiAutoStart = prefs.wifiAutoStart,
            wifiAutoStartSsid = prefs.wifiAutoStartSsid,
            resScale = prefs.resScale
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        try {
            prefs.syncHiddenApps()
        } catch (_: Throwable) {}
    }

    private var autoDimJob: kotlinx.coroutines.Job? = null
    private var isActivityForeground: Boolean = false

    fun onActivityResumed() {
        isActivityForeground = true
        resetAutoDimTimer()
    }

    fun onActivityPaused() {
        isActivityForeground = false
        autoDimJob?.cancel()
        autoDimJob = null
        bridge.dismissSystemDim()
    }

    fun onUserInteraction() {
        if (_uiState.value.isDimmed) {
            setDimmed(false)
        } else {
            resetAutoDimTimer()
        }
    }

    fun resetAutoDimTimer() {
        autoDimJob?.cancel()
        autoDimJob = null
        val sec = prefs.autoDimSeconds
        if (sec <= 0 || !isActivityForeground) return
        val isStreaming = bridge.connectionState.value == ConnectionState.STREAMING || bridge.connectionState.value == ConnectionState.CAR_CONNECTED
        if (!isStreaming || _uiState.value.isDimmed) return

        autoDimJob = viewModelScope.launch {
            delay(sec * 1000L)
            if (isActive && isActivityForeground && !_uiState.value.isDimmed) {
                val isStillStreaming = bridge.connectionState.value == ConnectionState.STREAMING || bridge.connectionState.value == ConnectionState.CAR_CONNECTED
                if (isStillStreaming) {
                    setDimmed(true)
                }
            }
        }
    }

    init {
        try {
            bridge.initUrl(TeslaDroidApp.instance)
        } catch (_: Throwable) {}

        viewModelScope.launch {
            bridge.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                val isStreaming = state == ConnectionState.STREAMING || state == ConnectionState.CAR_CONNECTED
                if (!isStreaming) {
                    if (_uiState.value.isDimmed) {
                        setDimmed(false)
                    }
                    autoDimJob?.cancel()
                    autoDimJob = null
                } else if (isActivityForeground) {
                    resetAutoDimTimer()
                }
            }
        }
        viewModelScope.launch {
            bridge.streamQuality.collect { quality ->
                _uiState.update { it.copy(streamQuality = quality) }
            }
        }
        viewModelScope.launch {
            bridge.audioMode.collect { mode ->
                _uiState.update { it.copy(audioMode = mode) }
            }
        }
        viewModelScope.launch {
            bridge.browserUrl.collect { url ->
                _uiState.update { it.copy(browserUrl = url) }
            }
        }
        viewModelScope.launch {
            bridge.currentFps.collect { fps ->
                _uiState.update { it.copy(fps = fps) }
            }
        }
        viewModelScope.launch {
            bridge.currentBitrateMbps.collect { bitrate ->
                _uiState.update { it.copy(bitrateMbps = bitrate) }
            }
        }
        viewModelScope.launch {
            bridge.connectedClientsCount.collect { count ->
                _uiState.update { it.copy(connectedClients = count) }
            }
        }
        viewModelScope.launch {
            bridge.shortUrl.collect { url ->
                _uiState.update { it.copy(shortUrl = url) }
            }
        }
    }

    fun refreshPermissions(context: Context) {
        SystemSettingsHelper.autoGrantPermissionsViaShizuku(context)
        bridge.initUrl(context)
        val devSettings = SystemSettingsHelper.isDevSettingsEnabled(context)
        val adbWifi = SystemSettingsHelper.isAdbWifiEnabled(context)
        val shizukuInstalled = SystemSettingsHelper.isShizukuInstalled(context)
        val shizukuRunning = SystemSettingsHelper.isShizukuRunning()
        val shizukuPermission = SystemSettingsHelper.checkShizukuPermission(context)
        val batteryGranted = SystemSettingsHelper.isIgnoringBatteryOptimizations(context)
        val notifications = SystemSettingsHelper.areNotificationsEnabled(context)
        val licenseActive = SystemSettingsHelper.isLicenseActive(context)

        val notifAccessGranted = try {
            val listeners = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            listeners?.contains(context.packageName) == true
        } catch (_: Throwable) { false }

        val btPermissionGranted = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        val btAdapter = try {
            val btManager = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
            btManager?.adapter ?: @Suppress("DEPRECATION") android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        } catch (_: Throwable) { null }

        val isBtEnabled = btAdapter?.isEnabled == true

        val pairedDevices = try {
            if (btPermissionGranted && btAdapter != null && isBtEnabled) {
                btAdapter.bondedDevices?.map { device ->
                    val name = try {
                        device.name?.takeIf { it.isNotBlank() } ?: "Unknown device"
                    } catch (_: SecurityException) {
                        "Device"
                    }
                    val addr = try { device.address ?: "" } catch (_: SecurityException) { "" }
                    name to addr
                }?.filter { it.second.isNotBlank() } ?: emptyList()
            } else emptyList()
        } catch (_: Throwable) { emptyList() }

        val currentWifiSsid = getCurrentWifiSsid(context)

        _uiState.update { current ->
            current.copy(
                isNotifAccessGranted = notifAccessGranted,
                isBtPermissionGranted = btPermissionGranted,
                isBtEnabled = isBtEnabled,
                pairedBtDevices = pairedDevices,
                currentConnectedWifiSsid = currentWifiSsid,
                permissionsState = current.permissionsState.copy(
                    devSettings = devSettings,
                    adbWifi = adbWifi,
                    shizukuInstalled = shizukuInstalled,
                    shizukuRunning = shizukuRunning,
                    shizukuPermission = shizukuPermission,
                    batteryOptimization = batteryGranted,
                    notifications = notifications,
                    licenseActive = licenseActive
                )
            )
        }
    }

    fun activateLicense(context: Context) {
        SystemSettingsHelper.activateLicense(context)
        bridge.initUrl(context)
        refreshPermissions(context)
    }

    fun setScreenCaptureGranted(granted: Boolean) {
        _uiState.update { current ->
            current.copy(
                permissionsState = current.permissionsState.copy(screenCapture = granted)
            )
        }
    }

    fun openSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun selectSettingsTab(index: Int) {
        _uiState.update { it.copy(selectedSettingsTab = index) }
    }

    fun setDimmed(dimmed: Boolean) {
        _uiState.update { it.copy(isDimmed = dimmed) }
        if (dimmed) {
            autoDimJob?.cancel()
            autoDimJob = null
            bridge.triggerSystemDim("button")
        } else {
            bridge.dismissSystemDim()
            if (isActivityForeground) {
                resetAutoDimTimer()
            }
        }
    }

    fun showAppPicker(show: Boolean, forMenu: Boolean = false, forKeepAlive: Boolean = false, forKeepPlaying: Boolean = false) {
        _uiState.update {
            it.copy(
                isAppPickerVisible = show,
                appPickerForMenu = forMenu,
                appPickerForKeepAlive = forKeepAlive,
                appPickerForKeepPlaying = forKeepPlaying
            )
        }
    }

    fun onAppPicked(app: InstalledAppItem?, forMenu: Boolean) {
        if (!forMenu) {
            val component = app?.componentName ?: ""
            prefs.autoLaunchPackage = component
            _uiState.update {
                it.copy(
                    autoLaunchPackage = component,
                    isAppPickerVisible = false
                )
            }
        } else {
            _uiState.update { it.copy(isAppPickerVisible = false) }
        }
    }

    fun toggleMenuApp(packageName: String) {
        val current = _uiState.value.selectedMenuApps.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.menuApps = current
        _uiState.update { it.copy(selectedMenuApps = current) }
    }

    fun setMenuApps(apps: Set<String>) {
        prefs.menuApps = apps
        _uiState.update { it.copy(selectedMenuApps = apps, isAppPickerVisible = false) }
    }

    fun setKeepAliveApps(apps: Set<String>) {
        prefs.keepAliveApps = apps
        _uiState.update { it.copy(keepAliveApps = apps, isAppPickerVisible = false) }
    }

    fun setKeepPlayingApps(apps: Set<String>) {
        prefs.keepPlayingApps = apps
        _uiState.update { it.copy(keepPlayingApps = apps, isAppPickerVisible = false) }
    }

    fun setWakeThreshold(threshold: Int) {
        prefs.wakeThreshold = threshold
        bridge.notifyWakeWordChanged(TeslaDroidApp.instance, prefs.wakeWordEnabled)
        _uiState.update { it.copy(wakeThreshold = threshold) }
    }

    fun setAutoStart(autoStart: Boolean) {
        prefs.autoStart = autoStart
        _uiState.update { it.copy(autoStart = autoStart) }
    }

    fun onVideoBitrateSelected(bitrate: VideoBitrate) {
        prefs.videoBitrateMbps = bitrate.mbps
        bridge.notifyBitrateChanged(bitrate.mbps)
        _uiState.update { it.copy(selectedVideoBitrate = bitrate) }
    }

    fun onAudioSettingModeSelected(mode: AudioSettingMode) {
        prefs.audioMode = mode.name
        val bridgeMode = when (mode) {
            AudioSettingMode.TESLA_BT -> AudioMode.TESLA_BT
            AudioSettingMode.WEB_OPUS -> AudioMode.WEB_AUDIO
            AudioSettingMode.WEB_PCM -> AudioMode.WEB_AUDIO
            AudioSettingMode.OFF -> AudioMode.OFF
        }
        bridge.updateAudioMode(bridgeMode)
        bridge.notifyAudioModeChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(selectedAudioSettingMode = mode) }
    }

    fun onAutoLaunchPackageSelected(packageName: String) {
        prefs.autoLaunchPackage = packageName
        _uiState.update { it.copy(autoLaunchPackage = packageName) }
    }


    fun setResScale(scale: Int) {
        prefs.resScale = scale
        _uiState.update { it.copy(resScale = scale) }
    }

    fun setMenuScale(scale: Int) {
        prefs.menuScale = scale
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(menuScale = scale) }
    }

    fun setShortCode(code: String) {
        val clean = code.trim()
        prefs.shortCode = clean
        _uiState.update { it.copy(shortCode = clean) }
        bridge.refreshShortCode(TeslaDroidApp.instance)
    }

    fun setAutoStopSeconds(sec: Int) {
        prefs.autoStopSeconds = sec
        _uiState.update { it.copy(autoStopSeconds = sec) }
    }

    fun setAutoDimSeconds(sec: Int) {
        prefs.autoDimSeconds = sec
        _uiState.update { it.copy(autoDimSeconds = sec) }
        resetAutoDimTimer()
    }

    fun setStripLeft(enabled: Boolean) {
        prefs.stripLeft = enabled
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(stripLeft = enabled) }
    }

    fun setPinRotation(enabled: Boolean) {
        prefs.pinRotation = enabled
        _uiState.update { it.copy(pinRotation = enabled) }
    }

    fun setDisplayDecorations(enabled: Boolean) {
        prefs.displayDecorations = enabled
        _uiState.update { it.copy(displayDecorations = enabled) }
    }

    fun setHideNavBar(enabled: Boolean) {
        prefs.hideNavBar = enabled
        _uiState.update { it.copy(hideNavBar = enabled) }
    }

    fun setOdometer(enabled: Boolean) {
        prefs.odometer = enabled
        bridge.notifyOdometerChanged(TeslaDroidApp.instance, enabled)
        _uiState.update { it.copy(odometer = enabled) }
    }

    fun setCarGpsNav(enabled: Boolean) {
        prefs.carGpsNav = enabled
        bridge.notifyCarGpsNavChanged(TeslaDroidApp.instance, enabled)
        _uiState.update { it.copy(carGpsNav = enabled) }
    }

    fun setDisplayDpi(dpi: Int) {
        prefs.displayDpi = dpi
        bridge.notifyDisplayDpiChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(displayDpi = dpi) }
    }

    fun setFpsSetting(fps: Int) {
        prefs.fps = fps
        _uiState.update { it.copy(fpsSetting = fps) }
    }

    fun setAudioPcm(pcm: Boolean) {
        prefs.audioPcm = pcm
        val mode = if (pcm) AudioSettingMode.WEB_PCM else AudioSettingMode.WEB_OPUS
        prefs.audioMode = mode.name
        bridge.updateAudioMode(AudioMode.WEB_AUDIO)
        bridge.notifyAudioModeChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(audioPcm = pcm, selectedAudioSettingMode = mode) }
    }

    fun setVideoDelayMs(delay: Int) {
        prefs.videoDelayMs = delay
        bridge.notifyVideoDelayChanged(delay)
        _uiState.update { it.copy(videoDelayMs = delay) }
    }

    fun setHotspot5Ghz(is5Ghz: Boolean) {
        prefs.hotspot5Ghz = is5Ghz
        _uiState.update { it.copy(hotspot5Ghz = is5Ghz) }
    }

    fun setAutoHotspot(enabled: Boolean) {
        prefs.autoHotspot = enabled
        _uiState.update { it.copy(autoHotspot = enabled) }
    }

    fun setInCarAssistantButtonEnabled(enabled: Boolean) {
        prefs.inCarAssistantButtonEnabled = enabled
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(inCarAssistantButtonEnabled = enabled) }
    }

    fun setGoogleAssistant(enabled: Boolean) {
        prefs.googleAssistant = enabled
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(googleAssistant = enabled) }
    }

    fun setVoiceCues(enabled: Boolean) {
        prefs.voiceCues = enabled
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(voiceCues = enabled) }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.wakeWordEnabled = enabled
        bridge.notifyWakeWordChanged(TeslaDroidApp.instance, enabled)
        _uiState.update { it.copy(wakeWordEnabled = enabled) }
    }

    fun setPhoneActions(enabled: Boolean) {
        prefs.phoneActions = enabled
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(phoneActions = enabled) }
    }

    fun setGlowSize(size: Int) {
        prefs.glowSize = size
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(glowSize = size) }
    }

    fun setGeminiApiKey(key: String) {
        prefs.geminiApiKey = key
        _uiState.update { it.copy(geminiApiKey = key) }
    }

    fun setGeminiModel(model: String) {
        prefs.geminiModel = model
        _uiState.update { it.copy(geminiModel = model) }
    }

    fun setGeminiVoice(voice: String) {
        prefs.geminiVoice = voice
        _uiState.update { it.copy(geminiVoice = voice) }
    }

    fun setHudShown(shown: Boolean) {
        prefs.hudShown = shown
        bridge.notifyPhoneConfigChanged(TeslaDroidApp.instance)
        _uiState.update { it.copy(hudShown = shown) }
    }

    fun showPermissionsSheet(show: Boolean) {
        _uiState.update { it.copy(isPermissionsSheetVisible = show) }
    }

    private var hasAutoStarted: Boolean = false
    private var userManuallyStopped: Boolean = false

    fun checkAndTriggerAutoStart(
        context: Context,
        onRequireVpnConsent: (Intent) -> Unit
    ) {
        val sp = context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
        val carBtPendingAt = sp.getLong("car_bt_pending_at", 0L)
        val diff = System.currentTimeMillis() - carBtPendingAt
        val isCarBtPending = carBtPendingAt > 0 && diff in 0..900000L

        if (carBtPendingAt > 0) {
            sp.edit().putLong("car_bt_pending_at", 0L).apply()
            try {
                val notifManager = context.getSystemService(android.app.NotificationManager::class.java)
                notifManager?.cancel(4107)
            } catch (_: Throwable) {}
        }

        val isTargetWifiConnected = if (prefs.wifiAutoStart && prefs.wifiAutoStartSsid.isNotBlank()) {
            val currentSsid = getCurrentWifiSsid(context)
            val targets = prefs.wifiAutoStartSsid.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
            targets.isNotEmpty() && currentSsid != null && targets.any { it.equals(currentSsid, ignoreCase = true) }
        } else false

        val shouldStart = isCarBtPending || ((isTargetWifiConnected || prefs.autoStart) && !hasAutoStarted && !userManuallyStopped)
        if (!shouldStart) return
        if (bridge.connectionState.value != ConnectionState.IDLE) return

        refreshPermissions(context)
        val perms = _uiState.value.permissionsState
        if (perms.shizukuRunning && perms.shizukuPermission) {
            hasAutoStarted = true
            if (!perms.licenseActive) {
                activateLicense(context)
            }
            val vpnConsent = bridge.checkVpnConsentRequired(context)
            if (vpnConsent != null) {
                onRequireVpnConsent(vpnConsent)
            } else {
                bridge.startShizukuStreaming(context)
            }
        } else {
            viewModelScope.launch {
                delay(600L)
                if ((isCarBtPending || !hasAutoStarted) && !userManuallyStopped && bridge.connectionState.value == ConnectionState.IDLE) {
                    refreshPermissions(context)
                    val p = _uiState.value.permissionsState
                    if (p.shizukuRunning && p.shizukuPermission) {
                        hasAutoStarted = true
                        if (!p.licenseActive) {
                            activateLicense(context)
                        }
                        val vpn = bridge.checkVpnConsentRequired(context)
                        if (vpn != null) {
                            onRequireVpnConsent(vpn)
                        } else {
                            bridge.startShizukuStreaming(context)
                        }
                    }
                }
            }
        }
    }

    fun toggleStreaming(
        context: Context,
        onRequireVpnConsent: (Intent) -> Unit
    ) {
        when (bridge.connectionState.value) {
            ConnectionState.IDLE, ConnectionState.ERROR -> {
                userManuallyStopped = false
                val perms = _uiState.value.permissionsState
                if (!perms.shizukuRunning) {
                    android.widget.Toast.makeText(
                        context,
                        "Shizuku service is not running. Launch Shizuku or open permissions checklist",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    showPermissionsSheet(true)
                    return
                }
                if (!perms.shizukuPermission) {
                    android.widget.Toast.makeText(
                        context,
                        "Please grant Shizuku permission",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    requestShizukuPermission(context)
                    return
                }
                if (!perms.licenseActive) {
                    activateLicense(context)
                }

                val vpnConsent = bridge.checkVpnConsentRequired(context)
                if (vpnConsent != null) {
                    onRequireVpnConsent(vpnConsent)
                    return
                }

                // Shizuku silent virtual display streaming without Android screen recording prompt
                bridge.startShizukuStreaming(context)
            }
            ConnectionState.STREAMING, ConnectionState.CAR_CONNECTED, ConnectionState.PREPARING -> {
                userManuallyStopped = true
                bridge.stopStreaming(context)
            }
        }
    }

    fun launchApp(context: Context, packageName: String) {
        SystemSettingsHelper.launchApp(context, packageName)
        bridge.resumeKeepPlayingApps(context, packageName)
    }

    fun openShizuku(context: Context) {
        SystemSettingsHelper.openShizuku(context)
    }

    fun requestShizukuPermission(context: Context) {
        SystemSettingsHelper.requestShizukuPermission()
        refreshPermissions(context)
    }

    fun openDevSettings(context: Context) {
        SystemSettingsHelper.openDevSettings(context)
    }

    fun openAdbWifi(context: Context) {
        SystemSettingsHelper.openAdbWifi(context)
    }

    fun openBatterySettings(context: Context) {
        SystemSettingsHelper.requestIgnoreBatteryOptimizations(context)
    }

    fun openNotifications(context: Context) {
        SystemSettingsHelper.openNotificationSettings(context)
    }

    // TeslaDroid 7.0: Bluetooth, Dimming, and Notification Setters
    fun setCarBluetooth(name: String, addr: String) {
        prefs.carBtName = name
        prefs.carBtAddr = addr
        if (addr.isNotBlank()) {
            prefs.autoStart = true
        }
        _uiState.update { it.copy(carBtName = name, carBtAddr = addr) }
    }

    fun setLockDim(enabled: Boolean) {
        prefs.lockDim = enabled
        _uiState.update { it.copy(lockDim = enabled) }
    }

    fun setPowerReveal(enabled: Boolean) {
        prefs.powerReveal = enabled
        _uiState.update { it.copy(powerReveal = enabled) }
    }

    fun setNotifPopup(mode: Int) {
        prefs.notifPopup = mode
        _uiState.update { it.copy(notifPopup = mode) }
    }

    fun setNotifText(enabled: Boolean) {
        prefs.notifText = enabled
        _uiState.update { it.copy(notifText = enabled) }
    }

    fun setNotifOngoing(enabled: Boolean) {
        prefs.notifOngoing = enabled
        _uiState.update { it.copy(notifOngoing = enabled) }
    }

    fun setMuteNotif(enabled: Boolean) {
        prefs.muteNotif = enabled
        _uiState.update { it.copy(muteNotif = enabled) }
    }

    fun setBtDevicePickerVisible(visible: Boolean) {
        _uiState.update { it.copy(isBtDevicePickerVisible = visible) }
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            SystemSettingsHelper.openNotificationSettings(context)
        }
    }

    fun setWifiAutoStart(enabled: Boolean) {
        prefs.wifiAutoStart = enabled
        _uiState.update { it.copy(wifiAutoStart = enabled) }
        if (enabled) {
            WifiAutoStartManager.checkAndTrigger(TeslaDroidApp.instance)
        }
    }

    fun setWifiAutoStartSsid(ssid: String) {
        val clean = ssid.trim().removeSurrounding("\"")
        prefs.wifiAutoStartSsid = clean
        _uiState.update { it.copy(wifiAutoStartSsid = clean) }
        if (prefs.wifiAutoStart && clean.isNotBlank()) {
            WifiAutoStartManager.checkAndTrigger(TeslaDroidApp.instance)
        }
    }

    fun getCurrentWifiSsid(context: Context): String? {
        return WifiAutoStartManager.getConnectedWifiSsid(context)
    }

    fun exportSessionLog(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val logFile = java.io.File(context.cacheDir, "diag.log")
                val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time"))
                val logs = p.inputStream.bufferedReader().use { it.readText() }
                val utcTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())
                logFile.writeText("=== TeslaDroid Diagnostic Log ===\nTimestamp: $utcTime\nDevice: Android ${android.os.Build.VERSION.RELEASE}\n\n$logs")

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    logFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "TeslaDroid Session Log")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Send Session Log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (t: Throwable) {
                android.util.Log.e("DashboardViewModel", "Export session log failed: ${t.message}")
            }
        }
    }

    fun captureSystemSnapshot(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val snapFile = java.io.File(context.cacheDir, "snapshot.txt")
                val sb = java.lang.StringBuilder()
                sb.append("=== TeslaDroid System Snapshot ===\n")
                sb.append("Timestamp: ${java.util.Date()}\n")
                sb.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (SDK ${android.os.Build.VERSION.SDK_INT})\n")
                sb.append("Session Token: ${bridge.getSessionToken(context)}\n")
                sb.append("Browser URL: ${bridge.getTeslaBrowserUrl(context)}\n\n")

                sb.append("--- DUMPSYS DISPLAY ---\n")
                sb.append(ShizukuBridge.execShell("dumpsys display | grep -E 'mDisplayId|mOverrideDisplayInfo|VirtualDisplay'"))
                sb.append("\n\n--- DUMPSYS ACTIVITIES ---\n")
                sb.append(ShizukuBridge.execShell("dumpsys activity activities | grep -E 'Display #|topResumedActivity'"))
                sb.append("\n\n--- NETWORK INTERFACES ---\n")
                sb.append(ShizukuBridge.execShell("ip addr"))

                snapFile.writeText(sb.toString())

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    snapFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "TeslaDroid System Snapshot")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share System Snapshot").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (t: Throwable) {
                android.util.Log.e("DashboardViewModel", "Capture snapshot failed: ${t.message}")
            }
        }
    }

    fun cleanCacheAndSessions(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            java.io.File(context.filesDir, "logs").deleteRecursively()
            ShizukuBridge.execShell("rm -rf /data/local/tmp/tesladroid-*")
            AppPreferences(context).syncAllToTeslaMirror()
            true
        } catch (t: Throwable) {
            android.util.Log.w("DashboardViewModel", "Clean cache error: ${t.message}")
            false
        }
    }
}
