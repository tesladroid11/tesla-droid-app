package com.tesladroid.ui.dashboard

import com.tesladroid.core.model.AudioMode
import com.tesladroid.core.model.AudioSettingMode
import com.tesladroid.core.model.ConnectionState
import com.tesladroid.core.model.PermissionsState
import com.tesladroid.core.model.StreamQuality
import com.tesladroid.core.model.VideoBitrate

data class DashboardUiState(
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val streamQuality: StreamQuality = StreamQuality.FHD_1080P,
    val audioMode: AudioMode = AudioMode.TESLA_BT,
    val browserUrl: String = "https://car.tesla-droid.app:8443",
    val fps: Int = 0,
    val bitrateMbps: Float = 0f,
    val connectedClients: Int = 0,
    val permissionsState: PermissionsState = PermissionsState(),
    val isPermissionsSheetVisible: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val selectedSettingsTab: Int = 0,
    
    // Quick controls & Screen dim
    val autoStart: Boolean = false,
    val isDimmed: Boolean = false,
    val selectedVideoBitrate: VideoBitrate = VideoBitrate.MBPS_8,
    val selectedAudioSettingMode: AudioSettingMode = AudioSettingMode.WEB_PCM,
    val autoLaunchPackage: String = "",
    val autoLaunchCustomPackage: String = "",
    val autoLaunchCustomLabel: String = "",
    val isAppPickerVisible: Boolean = false,
    val appPickerForMenu: Boolean = false,
    val appPickerForKeepAlive: Boolean = false,
    val appPickerForKeepPlaying: Boolean = false,
    val selectedMenuApps: Set<String> = emptySet(),
    val keepAliveApps: Set<String> = emptySet(),
    val keepPlayingApps: Set<String> = emptySet(),
    
    // Tab 1: Controls & Connectivity
    val autoStopSeconds: Int = 0,
    val autoDimSeconds: Int = 30,
    val menuScale: Int = 100,
    
    // Tab 2: Display
    val stripLeft: Boolean = true,
    val pinRotation: Boolean = true,
    val displayDecorations: Boolean = false,
    val hideNavBar: Boolean = false,
    val odometer: Boolean = false,
    val displayDpi: Int = 240,
    val fpsSetting: Int = 60,
    
    // Audio Stream & Hotspot (under Controls)
    val audioPcm: Boolean = true,
    val videoDelayMs: Int = 0,
    val hotspot5Ghz: Boolean = true,
    val autoHotspot: Boolean = true,
    
    // Tab 3: AI Assistant
    val carGpsNav: Boolean = false, // GPS Shield (Anti-Jamming & Spoofing Protection)
    val inCarAssistantButtonEnabled: Boolean = true,
    val googleAssistant: Boolean = true,
    val voiceCues: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val wakeThreshold: Int = 75,
    val phoneActions: Boolean = true,
    val glowSize: Int = 2,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash-native-audio-preview-12-2025",
    val geminiVoice: String = "Puck",
    
    val hudShown: Boolean = false,
    
    // TeslaDroid 7.0: Bluetooth Auto-start & Auto-stop
    val carBtAddr: String = "",
    val carBtName: String = "",
    val isBtDevicePickerVisible: Boolean = false,
    val isBtPermissionGranted: Boolean = false,
    val isBtEnabled: Boolean = true,
    val pairedBtDevices: List<Pair<String, String>> = emptyList(),

    // Wi-Fi Auto-start
    val wifiAutoStart: Boolean = false,
    val wifiAutoStartSsid: String = "",
    val currentConnectedWifiSsid: String? = null,

    // TeslaDroid 7.0: Locked Screen Dimming
    val lockDim: Boolean = true,
    val powerReveal: Boolean = false,

    // TeslaDroid 7.0: Car Notifications
    val notifPopup: Int = 1, // 0 = Off, 1 = Important, 2 = All
    val notifText: Boolean = true,
    val notifOngoing: Boolean = true,
    val muteNotif: Boolean = false,
    val isNotifAccessGranted: Boolean = false,

    // TeslaDroid 7.0: Short URL
    val shortUrl: String? = null,
    val shortCode: String = "",

    val resScale: Int = 100,

    val errorMessage: String? = null
)
