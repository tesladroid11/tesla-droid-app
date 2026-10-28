package com.tesladroid.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesladroid.core.model.VideoBitrate
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.ObsidianSurfaceHighlight
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TeslaCrimson
import com.tesladroid.theme.TeslaEmerald
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary
import com.tesladroid.ui.dashboard.DashboardUiState
import com.tesladroid.ui.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onClose: () -> Unit,
    onOpenMenuAppsPicker: () -> Unit,
    onOpenKeepAlivePicker: () -> Unit,
    onOpenKeepPlayingPicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissions(context)
        viewModel.setBtDevicePickerVisible(true)
        if (!isGranted) {
            Toast.makeText(
                context,
                "Nearby Devices permission required to detect vehicle Bluetooth",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val requestBtPermission = {
        if (Build.VERSION.SDK_INT >= 31) {
            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            viewModel.refreshPermissions(context)
        }
    }

    BackHandler {
        onClose()
    }

    val tabs = listOf(
        Pair("Controls", Icons.Default.Tune),
        Pair("Display", Icons.Default.Tv),
        Pair("AI Assistant", Icons.Default.Psychology),
        Pair("Diagnostics", Icons.Default.Build)
    )

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            SettingsTopBar(
                onBackClick = onClose
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Material 3 TabRow
            TabRow(
                selectedTabIndex = uiState.selectedSettingsTab,
                containerColor = ObsidianSurface,
                contentColor = TeslaAmber,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedSettingsTab]),
                        color = TeslaAmber
                    )
                },
                divider = {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ObsidianBorder))
                }
            ) {
                tabs.forEachIndexed { index, pair ->
                    val isSelected = uiState.selectedSettingsTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectSettingsTab(index) },
                        text = {
                            Text(
                                text = pair.first,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TeslaAmber else TextSecondary
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = pair.second,
                                contentDescription = pair.first,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) TeslaAmber else TextMuted
                            )
                        }
                    )
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.selectedSettingsTab) {
                    0 -> ControlTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRequestBtPermission = requestBtPermission,
                        onOpenMenuAppsPicker = onOpenMenuAppsPicker,
                        onOpenKeepAlivePicker = onOpenKeepAlivePicker,
                        onOpenKeepPlayingPicker = onOpenKeepPlayingPicker
                    )
                    1 -> DisplayTabContent(uiState, viewModel)
                    2 -> AiTabContent(uiState, viewModel)
                    3 -> DiagnosticsTabContent(uiState, viewModel)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (uiState.isBtDevicePickerVisible) {
        BluetoothDevicePickerDialog(
            pairedDevices = uiState.pairedBtDevices,
            selectedAddr = uiState.carBtAddr,
            isBtPermissionGranted = uiState.isBtPermissionGranted,
            isBtEnabled = uiState.isBtEnabled,
            onRequestPermission = requestBtPermission,
            onRefresh = { viewModel.refreshPermissions(context) },
            onSelect = { name, addr ->
                viewModel.setCarBluetooth(name, addr)
            },
            onDismiss = { viewModel.setBtDevicePickerVisible(false) }
        )
    }
}

@Composable
private fun SettingsTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(ObsidianSurface)
            .border(width = 1.dp, color = ObsidianBorder)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TeslaAmber,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = "SETTINGS",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
            Text(
                text = "TeslaDroid configuration & preferences",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

// ==========================================
// 1. TAB: CONTROL & CONNECTION
// ==========================================
@Composable
private fun ControlTabContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onRequestBtPermission: () -> Unit,
    onOpenMenuAppsPicker: () -> Unit,
    onOpenKeepAlivePicker: () -> Unit,
    onOpenKeepPlayingPicker: () -> Unit
) {
    val context = LocalContext.current

    // 0. Bluetooth Auto-Start with Car
    SettingsCard(title = "START ON VEHICLE BLUETOOTH", icon = Icons.Default.Radio) {
        val btSubtitle = if (uiState.carBtAddr.isEmpty()) {
            "Disabled. Select vehicle to start streaming automatically on Bluetooth connect"
        } else {
            "${uiState.carBtName} · start on unlock, stop after 30s disconnected"
        }
        SettingsActionButton(
            title = "Vehicle Bluetooth",
            subtitle = btSubtitle,
            icon = Icons.Default.Radio,
            onClick = {
                viewModel.refreshPermissions(context)
                if (Build.VERSION.SDK_INT >= 31 && !uiState.isBtPermissionGranted) {
                    onRequestBtPermission()
                } else {
                    viewModel.setBtDevicePickerVisible(true)
                }
            }
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Auto-start on Bluetooth",
            subtitle = "Start streaming automatically when vehicle connects, and stop when disconnected",
            checked = uiState.autoStart,
            onCheckedChange = { viewModel.setAutoStart(it) }
        )
    }

    // 0a. Wi-Fi Auto-Start
    SettingsCard(title = "AUTO-START ON WI-FI NETWORK", icon = Icons.Default.Wifi) {
        SettingsSwitchRow(
            title = "Auto-start on Wi-Fi connection",
            subtitle = "Start streaming automatically when connected to specified network (e.g. secondary phone hotspot or vehicle router)",
            checked = uiState.wifiAutoStart,
            onCheckedChange = { viewModel.setWifiAutoStart(it) }
        )

        if (uiState.wifiAutoStart) {
            SettingsDivider()
            val currentSsid = uiState.currentConnectedWifiSsid ?: viewModel.getCurrentWifiSsid(context)
            var ssidInput by remember(uiState.wifiAutoStartSsid) { mutableStateOf(uiState.wifiAutoStartSsid) }
            val focusManager = LocalFocusManager.current

            Text(
                text = "Network SSID (hotspot name):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = ssidInput,
                onValueChange = {
                    ssidInput = it
                    viewModel.setWifiAutoStartSsid(it)
                },
                placeholder = { Text("Wi-Fi network name...", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeslaAmber,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            if (!currentSsid.isNullOrBlank() && currentSsid != ssidInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurfaceHighlight)
                        .clickable {
                            ssidInput = currentSsid
                            viewModel.setWifiAutoStartSsid(currentSsid)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = TeslaAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use current: \"$currentSsid\"",
                        fontSize = 11.sp,
                        color = TeslaAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

        // 1. Audio & Hotspot
    SettingsCard(title = "AUDIO STREAM & HOTSPOT", icon = Icons.Default.GraphicEq) {
        SettingsOptionRow(
            title = "Video delay (ms)",
            subtitle = "Audio/video synchronization for Bluetooth delay compensation",
            options = listOf(
                Pair("0 ms", 0),
                Pair("50 ms", 50),
                Pair("100 ms", 100),
                Pair("150 ms", 150),
                Pair("200 ms", 200),
                Pair("300 ms", 300)
            ),
            selectedValue = uiState.videoDelayMs,
            onOptionSelected = { viewModel.setVideoDelayMs(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Stop hotspot when streaming ends",
            subtitle = "Hotspot turns on automatically when streaming begins. Turn off when streaming stops.",
            checked = uiState.autoHotspot,
            onCheckedChange = { viewModel.setAutoHotspot(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Wi-Fi Hotspot 5 GHz",
            subtitle = "Use 5 GHz band for lower latency vehicle connection",
            checked = uiState.hotspot5Ghz,
            onCheckedChange = { viewModel.setHotspot5Ghz(it) }
        )
    }

    // 2. Automation & Power
    SettingsCard(title = "AUTOMATION & POWER", icon = Icons.Default.Tune) {
        SettingsOptionRow(
            title = "Auto-stop streaming timer",
            subtitle = "Automatically ends session when no vehicle browser is connected",
            options = listOf(
                Pair("Off", 0),
                Pair("30 sec", 30),
                Pair("1 min", 60),
                Pair("2 min", 120),
                Pair("3 min", 180),
                Pair("5 min", 300)
            ),
            selectedValue = uiState.autoStopSeconds,
            onOptionSelected = { viewModel.setAutoStopSeconds(it) }
        )

        SettingsDivider()

        SettingsOptionRow(
            title = "Phone screen auto-dim",
            subtitle = "Reduces screen brightness to save battery and protect OLED display",
            options = listOf(
                Pair("Never", 0),
                Pair("10 sec", 10),
                Pair("20 sec", 20),
                Pair("30 sec", 30),
                Pair("40 sec", 40),
                Pair("50 sec", 50)
            ),
            selectedValue = uiState.autoDimSeconds,
            onOptionSelected = { viewModel.setAutoDimSeconds(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Dim lock screen",
            subtitle = "Session keeps screen awake for vehicle touch. Disabled: lock screen stays at standard brightness",
            checked = uiState.lockDim,
            onCheckedChange = { viewModel.setLockDim(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Power button reveals screen",
            subtitle = "When screen is dimmed, pressing power button wakes lock screen rather than turning screen off",
            checked = uiState.powerReveal,
            onCheckedChange = { viewModel.setPowerReveal(it) }
        )
    }

    // 3. Notifications on Car Screen
    SettingsCard(title = "NOTIFICATIONS ON CAR SCREEN", icon = Icons.Default.RecordVoiceOver) {
        if (!uiState.isNotifAccessGranted) {
            SettingsActionButton(
                title = "Notification access disabled",
                subtitle = "Tap here to grant notification listener access for vehicle display",
                icon = Icons.Default.CleaningServices,
                onClick = { viewModel.openNotificationListenerSettings(context) }
            )
            SettingsDivider()
        }

        SettingsOptionRow(
            title = "Car screen popup notifications",
            subtitle = "Show popup notification banners over windows in Tesla browser",
            options = listOf(
                Pair("Off", 0),
                Pair("Important only", 1),
                Pair("All", 2)
            ),
            selectedValue = uiState.notifPopup,
            onOptionSelected = { viewModel.setNotifPopup(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Show notification message text",
            subtitle = "Include message body. Disabled: show app icon and sender name only",
            checked = uiState.notifText,
            onCheckedChange = { viewModel.setNotifText(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Show ongoing notifications",
            subtitle = "Display charging, music, and monitoring alerts in status drawer",
            checked = uiState.notifOngoing,
            onCheckedChange = { viewModel.setNotifOngoing(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Mute phone notification sound",
            subtitle = "Phone does not play alert sounds while streaming — banners still show on phone and car",
            checked = uiState.muteNotif,
            onCheckedChange = { viewModel.setMuteNotif(it) }
        )
    }

    // 4. Browser Quick Menu & App Persistence
    SettingsCard(title = "TESLA BROWSER QUICK MENU", icon = Icons.Default.Apps) {
        SettingsActionButton(
            title = "Apps in menu",
            subtitle = if (uiState.selectedMenuApps.isEmpty()) "None selected (tap to choose)" else "Selected: ${uiState.selectedMenuApps.size} apps",
            icon = Icons.Default.Apps,
            onClick = onOpenMenuAppsPicker
        )

        SettingsDivider()

        SettingsActionButton(
            title = "Keep running after Stop",
            subtitle = if (uiState.keepAliveApps.isEmpty()) "All apps close after Stop" else "Selected: ${uiState.keepAliveApps.size} background apps",
            icon = Icons.Default.Tune,
            onClick = onOpenKeepAlivePicker
        )

        SettingsDivider()

        SettingsActionButton(
            title = "Don't pause media apps",
            subtitle = if (uiState.keepPlayingApps.isEmpty()) "All apps pause music" else "Selected: ${uiState.keepPlayingApps.size} apps (e.g. Waze, Maps)",
            icon = Icons.Default.GraphicEq,
            onClick = onOpenKeepPlayingPicker
        )
    }
}

// ==========================================
// ==========================================
// 2. TAB: DISPLAY
// ==========================================
@Composable
private fun DisplayTabContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel
) {
    SettingsCard(title = "TESLA SCREEN DISPLAY", icon = Icons.Default.Tv) {
        SettingsSwitchRow(
            title = "Left sidebar (Strip left)",
            subtitle = "Dock menu bar on left side for Left-Hand Drive vehicles",
            checked = uiState.stripLeft,
            onCheckedChange = { viewModel.setStripLeft(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Lock screen orientation (Pin rotation)",
            subtitle = "Prevent display rotation during active streaming",
            checked = uiState.pinRotation,
            onCheckedChange = { viewModel.setPinRotation(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Window decorations",
            subtitle = "Show window title bars and controls in multi-window mode",
            checked = uiState.displayDecorations,
            onCheckedChange = { viewModel.setDisplayDecorations(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Hide navigation bar",
            subtitle = "Hide Android system navigation bar for full-screen mirroring",
            checked = uiState.hideNavBar,
            onCheckedChange = { viewModel.setHideNavBar(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Odometer & Speed overlay",
            subtitle = "Display vehicle speed and trip data on system overlay",
            checked = uiState.odometer,
            onCheckedChange = { viewModel.setOdometer(it) }
        )
    }

    SettingsCard(title = "RESOLUTION, DENSITY & MENU", icon = Icons.Default.Speed) {
        SettingsOptionRow(
            title = "Virtual Display Density (DPI)",
            subtitle = "Pixel density of the virtual screen streamed to Tesla",
            options = listOf(
                Pair("160 DPI", 160),
                Pair("180 DPI", 180),
                Pair("240 DPI (Default)", 240),
                Pair("310 DPI", 310),
                Pair("400 DPI", 400)
            ),
            selectedValue = uiState.displayDpi,
            onOptionSelected = { viewModel.setDisplayDpi(it) }
        )

        SettingsDivider()

        SettingsOptionRow(
            title = "Virtual Display Scaling (Render Scale)",
            subtitle = "Internal virtual display resolution scaling factor",
            options = listOf(
                Pair("80%", 80),
                Pair("90%", 90),
                Pair("100% (Native)", 100),
                Pair("110%", 110),
                Pair("125%", 125)
            ),
            selectedValue = uiState.resScale,
            onOptionSelected = { viewModel.setResScale(it) }
        )

        SettingsDivider()

        SettingsOptionRow(
            title = "Sidebar & Quick Menu Scale",
            subtitle = "Size of sidebar icons, controls and app tiles in the Tesla UI",
            options = listOf(
                Pair("80%", 80),
                Pair("100% (Default)", 100),
                Pair("120%", 120),
                Pair("140%", 140),
                Pair("160% (Large / HW4)", 160),
                Pair("180% (Extra Large)", 180),
                Pair("200% (Maximum)", 200)
            ),
            selectedValue = uiState.menuScale,
            onOptionSelected = { viewModel.setMenuScale(it) }
        )
    }

    SettingsCard(title = "VIDEO STREAMING & BITRATE", icon = Icons.Default.Tune) {
        SettingsOptionRow(
            title = "Video Bitrate",
            subtitle = "Stream sharpness and network bandwidth",
            options = listOf(
                Pair("Auto", VideoBitrate.AUTO),
                Pair("4 Mbps", VideoBitrate.MBPS_4),
                Pair("6 Mbps", VideoBitrate.MBPS_6),
                Pair("8 Mbps", VideoBitrate.MBPS_8),
                Pair("12 Mbps", VideoBitrate.MBPS_12),
                Pair("16 Mbps", VideoBitrate.MBPS_16),
                Pair("20 Mbps", VideoBitrate.MBPS_20)
            ),
            selectedValue = uiState.selectedVideoBitrate,
            onOptionSelected = { viewModel.onVideoBitrateSelected(it) }
        )

        SettingsDivider()

        SettingsOptionRow(
            title = "Target Frame Rate (FPS)",
            subtitle = "Video frame refresh rate on Tesla browser",
            options = listOf(
                Pair("20 FPS", 20),
                Pair("30 FPS", 30),
                Pair("40 FPS", 40),
                Pair("60 FPS", 60)
            ),
            selectedValue = uiState.fpsSetting,
            onOptionSelected = { viewModel.setFpsSetting(it) }
        )

        SettingsDivider()

        SettingsOptionRow(
            title = "Vehicle Audio Quality / Codec",
            subtitle = "Web Audio stream format delivered to browser",
            options = listOf(
                Pair("Opus (Compressed)", false),
                Pair("Lossless PCM", true)
            ),
            selectedValue = uiState.audioPcm,
            onOptionSelected = { viewModel.setAudioPcm(it) }
        )
    }
}

// ==========================================
// 3. TAB: AI ASSISTANT
// ==========================================
@Composable
private fun AiTabContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current

    // 1. GPS Shield (Anti-Jamming & Spoofing Protection) - Moved to top of AI Tab
    SettingsCard(title = "NAVIGATION & GPS PROTECTION", icon = Icons.Default.Navigation) {
        SettingsSwitchRow(
            title = "GPS Shield (Anti-Jamming & Spoofing Protection)",
            subtitle = "Protects navigation apps against GPS jamming and spoofing anomalies by seamlessly failing over to cellular and Wi-Fi positioning.",
            checked = uiState.carGpsNav,
            onCheckedChange = { viewModel.setCarGpsNav(it) }
        )
    }

    // 2. Two distinct toggles: Gemini in car browser & Google Gemini
    SettingsCard(title = "IN-CABIN AI INTEGRATION", icon = Icons.Default.Psychology) {
        SettingsSwitchRow(
            title = "✦ Button in car browser",
            subtitle = "Show interactive floating AI button on vehicle touchscreen",
            checked = uiState.inCarAssistantButtonEnabled,
            onCheckedChange = { viewModel.setInCarAssistantButtonEnabled(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Google Gemini",
            subtitle = "Connect Google multimodal intelligent voice assistant",
            checked = uiState.googleAssistant,
            onCheckedChange = { viewModel.setGoogleAssistant(it) }
        )
    }

    // 3. Voice Cues & Wake Word & Phone Actions
    SettingsCard(title = "VOICE ASSISTANCE & CUES", icon = Icons.Default.RecordVoiceOver) {
        SettingsSwitchRow(
            title = "Voice Cues",
            subtitle = "Speak turn-by-turn prompts and system notifications aloud",
            checked = uiState.voiceCues,
            onCheckedChange = { viewModel.setVoiceCues(it) }
        )

        SettingsDivider()

        SettingsSwitchRow(
            title = "Wake Word (\"Hey Tesla\")",
            subtitle = "Hands-free voice assistant activation",
            checked = uiState.wakeWordEnabled,
            onCheckedChange = { viewModel.setWakeWordEnabled(it) }
        )

        if (uiState.wakeWordEnabled) {
            SettingsDivider()

            SettingsOptionRow(
                title = "Wake Word Sensitivity",
                subtitle = "Detection threshold: ${uiState.wakeThreshold}% (higher = fewer false triggers)",
                options = listOf(
                    Pair("50%", 50),
                    Pair("60%", 60),
                    Pair("70%", 70),
                    Pair("75%", 75),
                    Pair("80%", 80),
                    Pair("85%", 85),
                    Pair("90%", 90),
                    Pair("95%", 95)
                ),
                selectedValue = uiState.wakeThreshold,
                onOptionSelected = { viewModel.setWakeThreshold(it) }
            )
        }

        SettingsDivider()

        SettingsSwitchRow(
            title = "Phone actions",
            subtitle = "Allow assistant to control media, navigation, and system tasks",
            checked = uiState.phoneActions,
            onCheckedChange = { viewModel.setPhoneActions(it) }
        )
    }

    // 4. Visual Glow Effects
    SettingsCard(title = "VISUAL EFFECTS", icon = Icons.Default.AutoAwesome) {
        SettingsSwitchRow(
            title = "Screen edge glow",
            subtitle = "Animated neon outline around screen during assistant interaction",
            checked = uiState.glowSize > 0,
            onCheckedChange = { checked ->
                viewModel.setGlowSize(if (checked) 50 else 0)
            }
        )
    }

    // 5. Assistant Voice Personality
    SettingsCard(title = "ASSISTANT VOICE", icon = Icons.Default.Psychology) {
        SettingsOptionRow(
            title = "Gemini Voice",
            subtitle = "Assistant speech timbre and voice personality",
            options = listOf(
                Pair("Puck", "Puck"),
                Pair("Charon", "Charon"),
                Pair("Aoede", "Aoede"),
                Pair("Fenrir", "Fenrir"),
                Pair("Kore", "Kore")
            ),
            selectedValue = uiState.geminiVoice,
            onOptionSelected = { viewModel.setGeminiVoice(it) }
        )
    }
}

// ==========================================
// 4. TAB: DIAGNOSTICS
// ==========================================
@Composable
private fun DiagnosticsTabContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current

    SettingsCard(title = "SYSTEM ACTIONS", icon = Icons.Default.Build) {
        SettingsSwitchRow(
            title = "HUD Performance Overlay",
            subtitle = "Show floating widget with FPS, bitrate, and network status",
            checked = uiState.hudShown,
            onCheckedChange = { viewModel.setHudShown(it) }
        )

        SettingsDivider()

        SettingsActionButton(
            title = "Send Session Log",
            subtitle = "Export system diagnostic log to resolve issues",
            icon = Icons.Default.Send,
            onClick = {
                viewModel.exportSessionLog(context)
            }
        )

        SettingsDivider()

        SettingsActionButton(
            title = "Capture System Snapshot",
            subtitle = "Save complete device telemetry and network dump",
            icon = Icons.Default.Timer,
            onClick = {
                viewModel.captureSystemSnapshot(context)
            }
        )

        SettingsDivider()

        SettingsActionButton(
            title = "Clean Cache & Session Data",
            subtitle = "Reset saved network sessions and temporary cache files",
            icon = Icons.Default.CleaningServices,
            accentColor = TeslaCrimson,
            onClick = {
                val ok = viewModel.cleanCacheAndSessions(context)
                Toast.makeText(context, if (ok) "Cache successfully cleared!" else "Error clearing cache", Toast.LENGTH_SHORT).show()
            }
        )
    }

    SettingsCard(title = "SYSTEM COMPONENT STATUS", icon = Icons.Default.Radio) {
        StatusRow(label = "Local Web Server", value = "Port 8443 (HTTPS + WSS)", isOk = true)
        StatusRow(label = "TLS Certificate", value = "*.car.tesla-droid.app (Trusted)", isOk = true)
        StatusRow(label = "Shizuku Touch Input", value = if (uiState.permissionsState.shizukuPermission) "Connected (/dev/input)" else "Awaiting Shizuku", isOk = uiState.permissionsState.shizukuPermission)
        StatusRow(label = "Engine Activation", value = if (uiState.permissionsState.licenseActive) "Active (Ready)" else "Ready", isOk = true)
        StatusRow(label = "Hardware Codec", value = "MediaCodec H.265 / HEVC", isOk = true)
        StatusRow(label = "Core Engine", value = "TeslaDroid 1.0.0 (Native CIO)", isOk = true)
    }
}

// ==========================================
// REUSABLE SETTINGS COMPONENTS
// ==========================================

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TeslaAmber,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = TeslaAmber,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianBlack
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SettingsOptionRow(
    title: String,
    subtitle: String,
    options: List<Pair<String, T>>,
    selectedValue: T,
    onOptionSelected: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { (label, value) ->
                val isSelected = value == selectedValue
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) TeslaAmber else ObsidianBlack)
                        .border(
                            1.dp,
                            if (isSelected) TeslaAmber else ObsidianBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onOptionSelected(value) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ObsidianBlack else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = TeslaAmber,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isOk) TeslaEmerald else TeslaAmber
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isOk) TeslaEmerald else TeslaAmber)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(ObsidianBorder.copy(alpha = 0.6f))
    )
}

@Composable
private fun BluetoothDevicePickerDialog(
    pairedDevices: List<Pair<String, String>>,
    selectedAddr: String,
    isBtPermissionGranted: Boolean,
    isBtEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (name: String, addr: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vehicle Bluetooth",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TeslaAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Warning if Bluetooth permission missing on Android 12+
                if (!isBtPermissionGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeslaCrimson.copy(alpha = 0.15f))
                            .border(1.dp, TeslaCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Nearby Devices permission not granted",
                                color = TeslaCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Bluetooth permission is required to detect paired vehicles.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.Button(
                                    onClick = onRequestPermission,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = TeslaCrimson
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Grant Permission", fontSize = 12.sp, color = Color.White)
                                }
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Throwable) {}
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Settings", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // Warning if Bluetooth is turned off
                if (!isBtEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeslaAmber.copy(alpha = 0.15f))
                            .border(1.dp, TeslaAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Bluetooth is turned off",
                                color = TeslaAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Enable Bluetooth on your phone to view paired devices.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            androidx.compose.material3.Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Throwable) {}
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = TeslaAmber
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Enable Bluetooth", fontSize = 12.sp, color = ObsidianBlack)
                            }
                        }
                    }
                }

                // Option: "Disabled"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedAddr.isEmpty()) TeslaAmber.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (selectedAddr.isEmpty()) TeslaAmber else ObsidianBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect("", ""); onDismiss() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Disabled",
                            color = if (selectedAddr.isEmpty()) TeslaAmber else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (selectedAddr.isEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "Bluetooth auto-start is disabled",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (selectedAddr.isEmpty()) {
                        Text(
                            text = "✓",
                            color = TeslaAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                if (isBtPermissionGranted && isBtEnabled) {
                    if (pairedDevices.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBorder.copy(alpha = 0.2f))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No paired Bluetooth devices",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Pair your phone with your Tesla in Android Bluetooth settings first.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Throwable) {}
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Bluetooth Settings", color = TeslaAmber, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "Select your vehicle Bluetooth:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        pairedDevices.forEach { (name, addr) ->
                            val isSelected = selectedAddr.equals(addr, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) TeslaAmber.copy(alpha = 0.18f) else ObsidianBorder.copy(alpha = 0.25f))
                                    .border(1.dp, if (isSelected) TeslaAmber else ObsidianBorder, RoundedCornerShape(10.dp))
                                    .clickable { onSelect(name, addr); onDismiss() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = if (isSelected) TeslaAmber else TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = addr,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = TeslaAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close", color = TeslaAmber)
            }
        }
    )
}
