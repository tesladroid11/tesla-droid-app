package com.tesladroid.ui.dashboard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tesladroid.R
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TeslaEmerald
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary
import com.tesladroid.ui.dashboard.components.AppPickerBottomSheet
import com.tesladroid.ui.dashboard.components.AudioSelectorCard
import com.tesladroid.ui.dashboard.components.BrowserLinkCard
import com.tesladroid.ui.dashboard.components.HeroStatusCard
import com.tesladroid.ui.dashboard.components.PermissionsBottomSheet
import com.tesladroid.ui.dashboard.components.QuickLaunchBar
import com.tesladroid.ui.dashboard.components.VideoParametersCard
import com.tesladroid.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onRequireVpnConsent: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = ObsidianBlack,
            topBar = {
                CockpitTopBar(
                    allCrucialPermissionsGranted = uiState.permissionsState.allCrucialGranted,
                    onCarIconClick = { viewModel.openSettings(true) },
                    onPermissionsClick = { viewModel.showPermissionsSheet(true) }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. Full / Short Tesla Browser URL Link Card (At the very top)
                BrowserLinkCard(
                    url = uiState.browserUrl,
                    shortUrl = uiState.shortUrl
                )

                // 2. Hero Status Card (Big Power Button + 50% Auto-start + 50% Dim Screen)
                HeroStatusCard(
                    connectionState = uiState.connectionState,
                    autoStart = uiState.autoStart,
                    onAutoStartChanged = { viewModel.setAutoStart(it) },
                    autoHotspot = uiState.autoHotspot,
                    onAutoHotspotChanged = { viewModel.setAutoHotspot(it) },
                    onDimScreenClick = { viewModel.setDimmed(true) },
                    onPowerButtonClick = {
                        viewModel.toggleStreaming(
                            context = context,
                            onRequireVpnConsent = onRequireVpnConsent
                        )
                    }
                )

                // 3. Compact Dropdowns: Bitrate, FPS, DPI, Menu Scale
                VideoParametersCard(
                    selectedBitrate = uiState.selectedVideoBitrate,
                    onBitrateSelected = { viewModel.onVideoBitrateSelected(it) },
                    selectedFps = uiState.fpsSetting,
                    onFpsSelected = { viewModel.setFpsSetting(it) },
                    selectedDpi = uiState.displayDpi,
                    onDpiSelected = { viewModel.setDisplayDpi(it) },
                    selectedMenuScale = uiState.menuScale,
                    onMenuScaleSelected = { viewModel.setMenuScale(it) }
                )

                // 4. Audio Quality Selector (Uncompressed vs Compressed Audio)
                AudioSelectorCard(
                    isAudioPcm = uiState.audioPcm,
                    onAudioModeChanged = { viewModel.setAudioPcm(it) }
                )

                // 5. Default Launch App (Waze, Maps, Custom App, Menu with long-press)
                QuickLaunchBar(
                    selectedAutoLaunchPackage = uiState.autoLaunchPackage,
                    onAutoLaunchToggled = { pkg ->
                        viewModel.onAutoLaunchPackageSelected(pkg)
                    },
                    onOpenAppPicker = {
                        viewModel.showAppPicker(true, forMenu = false)
                    },
                    onOpenMenuAppsPicker = {
                        viewModel.showAppPicker(true, forMenu = true)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6. System Permissions Modal BottomSheet
            if (uiState.isPermissionsSheetVisible) {
                PermissionsBottomSheet(
                    sheetState = permissionsSheetState,
                    permissionsState = uiState.permissionsState,
                    onDismiss = { viewModel.showPermissionsSheet(false) },
                    onRequestDevSettings = { viewModel.openDevSettings(context) },
                    onRequestAdbWifi = { viewModel.openAdbWifi(context) },
                    onOpenShizuku = { viewModel.openShizuku(context) },
                    onRequestShizukuPermission = { viewModel.requestShizukuPermission(context) },
                    onRequestBattery = { viewModel.openBatterySettings(context) },
                    onRequestNotifications = { viewModel.openNotifications(context) },
                    onActivateLicense = { viewModel.activateLicense(context) }
                )
            }

            // 7. App Picker Modal BottomSheet (For picking installed apps)
            if (uiState.isAppPickerVisible) {
                AppPickerBottomSheet(
                    sheetState = appPickerSheetState,
                    currentSelectedPackage = uiState.autoLaunchPackage,
                    isMultiSelect = uiState.appPickerForMenu || uiState.appPickerForKeepAlive || uiState.appPickerForKeepPlaying,
                    selectedPackages = when {
                        uiState.appPickerForKeepPlaying -> uiState.keepPlayingApps
                        uiState.appPickerForKeepAlive -> uiState.keepAliveApps
                        else -> uiState.selectedMenuApps
                    },
                    title = when {
                        uiState.appPickerForKeepPlaying -> "DON'T PAUSE MEDIA APPS"
                        uiState.appPickerForKeepAlive -> "KEEP RUNNING AFTER STOP"
                        uiState.appPickerForMenu -> "TESLA MENU APPS"
                        else -> "AUTORUN APP ON CONNECT"
                    },
                    subtitle = when {
                        uiState.appPickerForKeepPlaying -> "Select apps that should not pause background music when launched (e.g. Waze, Google Maps)"
                        uiState.appPickerForKeepAlive -> "Select apps to keep running in background after pressing Stop"
                        uiState.appPickerForMenu -> "Select apps to show in the vehicle screen quick menu"
                        else -> "Select application to automatically launch on the Tesla display"
                    },
                    onAppSelected = { app ->
                        viewModel.onAppPicked(app, false)
                    },
                    onMultiSelectSaved = { apps ->
                        when {
                            uiState.appPickerForKeepPlaying -> viewModel.setKeepPlayingApps(apps)
                            uiState.appPickerForKeepAlive -> viewModel.setKeepAliveApps(apps)
                            else -> viewModel.setMenuApps(apps)
                        }
                    },
                    onDismiss = { viewModel.showAppPicker(false) }
                )
            }
        }

        // 8. Fullscreen Settings View with 4 tabs when opened by clicking car icon
        AnimatedVisibility(
            visible = uiState.isSettingsOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            SettingsScreen(
                uiState = uiState,
                viewModel = viewModel,
                onClose = { viewModel.openSettings(false) },
                onOpenMenuAppsPicker = {
                    viewModel.showAppPicker(true, forMenu = true, forKeepAlive = false, forKeepPlaying = false)
                },
                onOpenKeepAlivePicker = {
                    viewModel.showAppPicker(true, forMenu = false, forKeepAlive = true, forKeepPlaying = false)
                },
                onOpenKeepPlayingPicker = {
                    viewModel.showAppPicker(true, forMenu = false, forKeepAlive = false, forKeepPlaying = true)
                }
            )
        }

        // 9. OLED Full-Screen Dim Overlay (100% pitch-black, tap anywhere to wake)
        if (uiState.isDimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.setDimmed(false) }
                    )
            )
        }
    }
}

@Composable
private fun CockpitTopBar(
    allCrucialPermissionsGranted: Boolean,
    onCarIconClick: () -> Unit,
    onPermissionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Branding & Car Icon button (Opens 4-Tab Settings Screen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = TeslaAmber),
                    onClick = onCarIconClick
                )
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, TeslaAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_minimal_titanium),
                    contentDescription = "Tesla Settings",
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TESLA",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "DROID",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TeslaAmber
                    )
                }
                Text(
                    text = "YOUR ANDROID MIRROR IN TESLA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextMuted
                )
            }
        }

        // Right Action: Permissions Status Badge Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianSurface)
                .border(
                    1.dp,
                    if (allCrucialPermissionsGranted) ObsidianBorder else TeslaAmber.copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onPermissionsClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (allCrucialPermissionsGranted) Icons.Default.Security else Icons.Default.Warning,
                    contentDescription = "Permissions",
                    tint = if (allCrucialPermissionsGranted) TeslaEmerald else TeslaAmber,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (allCrucialPermissionsGranted) "PERMISSIONS OK" else "PERMISSIONS (!)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (allCrucialPermissionsGranted) TextSecondary else TeslaAmber
                )
            }
        }
    }
}
