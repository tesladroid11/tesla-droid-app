package com.tesladroid.ui.dashboard.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.tesladroid.R
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.ObsidianSurfaceHighlight
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TeslaCyan
import com.tesladroid.theme.TeslaEmerald
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickLaunchBar(
    selectedAutoLaunchPackage: String,
    onAutoLaunchToggled: (String) -> Unit,
    onOpenAppPicker: () -> Unit,
    onOpenMenuAppsPicker: () -> Unit = onOpenAppPicker,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wazePkg = "com.waze"
    val mapsPkg = "com.google.android.apps.maps"

    val isWazeSelected = selectedAutoLaunchPackage.contains(wazePkg)
    val isMapsSelected = selectedAutoLaunchPackage.contains(mapsPkg)
    val isCustomSelected = selectedAutoLaunchPackage.isNotEmpty() && !isWazeSelected && !isMapsSelected

    var customAppLabel by remember(selectedAutoLaunchPackage) { mutableStateOf("") }
    var customAppIcon by remember(selectedAutoLaunchPackage) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(selectedAutoLaunchPackage) {
        if (isCustomSelected) {
            withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val pkg = selectedAutoLaunchPackage.substringBefore('/')
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    customAppLabel = pm.getApplicationLabel(appInfo).toString()
                    customAppIcon = pm.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    customAppLabel = selectedAutoLaunchPackage.substringBefore('/').substringAfterLast('.')
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TeslaAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = TeslaAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APP ON CONNECT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (selectedAutoLaunchPackage.isEmpty()) "DISABLED" else "ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedAutoLaunchPackage.isEmpty()) TextMuted else TeslaAmber
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to set auto-launch. Long-press Menu or app tile to pick any installed application:",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Waze Tile
                QuickLaunchTile(
                    name = "Waze",
                    category = "Navigation",
                    iconVector = Icons.Default.Navigation,
                    accentColor = TeslaCyan,
                    isSelected = isWazeSelected,
                    onClick = {
                        val target = if (isWazeSelected) "" else "com.waze/.FreeMapAppActivity"
                        onAutoLaunchToggled(target)
                    },
                    onLongClick = onOpenAppPicker,
                    modifier = Modifier.weight(1f)
                )

                // 2. Google Maps Tile
                QuickLaunchTile(
                    name = "Maps",
                    category = "Maps",
                    iconVector = Icons.Default.Explore,
                    accentColor = TeslaEmerald,
                    isSelected = isMapsSelected,
                    onClick = {
                        val target = if (isMapsSelected) "" else "com.google.android.apps.maps/com.google.android.maps.MapsActivity"
                        onAutoLaunchToggled(target)
                    },
                    onLongClick = onOpenAppPicker,
                    modifier = Modifier.weight(1f)
                )

                // 3. Custom Chosen App (or "+ Add" if none)
                if (isCustomSelected && customAppLabel.isNotEmpty()) {
                    QuickLaunchTile(
                        name = customAppLabel,
                        category = "Selected",
                        customDrawable = customAppIcon,
                        accentColor = TeslaAmber,
                        isSelected = true,
                        onClick = {
                            onAutoLaunchToggled("")
                        },
                        onLongClick = onOpenAppPicker,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    QuickLaunchTile(
                        name = "+ Add",
                        category = "Custom",
                        iconVector = Icons.Default.Add,
                        accentColor = TextMuted,
                        isSelected = false,
                        onClick = onOpenAppPicker,
                        onLongClick = onOpenAppPicker,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Menu (AppPicker)
                QuickLaunchTile(
                    name = "Menu",
                    category = "All Apps",
                    iconRes = R.drawable.ic_apps,
                    accentColor = TeslaAmber,
                    isSelected = false,
                    onClick = onOpenMenuAppsPicker,
                    onLongClick = onOpenMenuAppsPicker,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickLaunchTile(
    name: String,
    category: String,
    iconVector: ImageVector? = null,
    iconRes: Int? = null,
    customDrawable: Drawable? = null,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ObsidianSurfaceHighlight else ObsidianBlack)
            .border(
                1.dp,
                if (isSelected) TeslaAmber else ObsidianBorder.copy(alpha = 0.8f),
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isSelected) 0.25f else 0.12f))
                    .border(
                        1.dp,
                        if (isSelected) accentColor else accentColor.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    customDrawable != null -> {
                        Image(
                            bitmap = customDrawable.toBitmap(72, 72).asImageBitmap(),
                            contentDescription = name,
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                        )
                    }
                    iconRes != null -> {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = name,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    iconVector != null -> {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = name,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TeslaAmber else TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (isSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        tint = TeslaEmerald,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "AUTO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeslaEmerald
                    )
                }
            } else {
                Text(
                    text = category,
                    fontSize = 8.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}
