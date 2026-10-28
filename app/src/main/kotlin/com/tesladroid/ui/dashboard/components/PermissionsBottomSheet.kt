package com.tesladroid.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesladroid.core.model.PermissionsState
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TeslaEmerald
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    sheetState: SheetState,
    permissionsState: PermissionsState,
    onDismiss: () -> Unit,
    onRequestDevSettings: () -> Unit,
    onRequestAdbWifi: () -> Unit,
    onOpenShizuku: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestNotifications: () -> Unit,
    onActivateLicense: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ObsidianBorder)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "READINESS CHECKLIST",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "All required system permissions and services for car display",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ObsidianBlack)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Developer Options
            PermissionRow(
                title = "1. Developer Options",
                subtitle = "Required for wireless debugging and Shizuku operation",
                icon = Icons.Default.Build,
                isGranted = permissionsState.devSettings,
                onActionClick = onRequestDevSettings,
                actionLabel = "Open"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Wireless Debugging
            PermissionRow(
                title = "2. Wireless Debugging (ADB)",
                subtitle = "Allows local wireless connection to system services",
                icon = Icons.Default.Wifi,
                isGranted = permissionsState.adbWifi,
                onActionClick = onRequestAdbWifi,
                actionLabel = "Enable"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Shizuku Installed
            PermissionRow(
                title = "3. Shizuku Installed",
                subtitle = "Provides screen control and touch injection without root",
                icon = Icons.Default.Terminal,
                isGranted = permissionsState.shizukuInstalled,
                onActionClick = onOpenShizuku,
                actionLabel = if (permissionsState.shizukuInstalled) "Open" else "Install"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Shizuku Service Running
            PermissionRow(
                title = "4. Shizuku Service Running",
                subtitle = "Privileged background service must be active to create virtual displays",
                icon = Icons.Default.Terminal,
                isGranted = permissionsState.shizukuRunning,
                onActionClick = onOpenShizuku,
                actionLabel = "Start"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Shizuku Permission for TeslaDroid
            PermissionRow(
                title = "5. Shizuku Permission Granted",
                subtitle = "Grants TeslaDroid access to interact with the vehicle display",
                icon = Icons.Default.Security,
                isGranted = permissionsState.shizukuPermission,
                onActionClick = onRequestShizukuPermission,
                actionLabel = "Grant"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Background Activity (Battery)
            PermissionRow(
                title = "6. Background Activity (Battery Optimization)",
                subtitle = "Prevents Android from killing the background stream while driving",
                icon = Icons.Default.BatteryAlert,
                isGranted = permissionsState.batteryOptimization,
                onActionClick = onRequestBattery,
                actionLabel = "Disable Limit"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Notifications
            PermissionRow(
                title = "7. Stream Service Notifications",
                subtitle = "Keeps the streaming foreground service alive in Android",
                icon = Icons.Default.Notifications,
                isGranted = permissionsState.notifications,
                onActionClick = onRequestNotifications,
                actionLabel = "Allow"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 8. Engine Readiness (One-click instant activation)
            PermissionRow(
                title = "8. Engine Readiness",
                subtitle = if (permissionsState.licenseActive) "TeslaDroid core client is activated and ready." else "Tap for instant one-click activation",
                icon = Icons.Default.VerifiedUser,
                isGranted = permissionsState.licenseActive,
                onActionClick = onActivateLicense,
                actionLabel = "Ready"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeslaAmber,
                    contentColor = ObsidianBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "CLOSE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    actionLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBlack)
            .border(
                width = 1.dp,
                color = if (isGranted) TeslaEmerald.copy(alpha = 0.35f) else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isGranted) TeslaEmerald.copy(alpha = 0.15f)
                    else TeslaAmber.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) TeslaEmerald else TeslaAmber,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TeslaEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = TeslaEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TeslaAmber.copy(alpha = 0.18f))
                    .border(1.dp, TeslaAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeslaAmber
                )
            }
        }
    }
}
