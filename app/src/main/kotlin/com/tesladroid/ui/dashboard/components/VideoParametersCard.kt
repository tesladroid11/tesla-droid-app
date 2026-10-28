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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesladroid.core.model.VideoBitrate
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary

@Composable
fun VideoParametersCard(
    selectedBitrate: VideoBitrate,
    onBitrateSelected: (VideoBitrate) -> Unit,
    selectedFps: Int,
    onFpsSelected: (Int) -> Unit,
    selectedDpi: Int,
    onDpiSelected: (Int) -> Unit,
    selectedMenuScale: Int,
    onMenuScaleSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TeslaAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TeslaAmber,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VIDEO & DISPLAY PARAMETERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )
            }

            // Row 1: BITRATE + FPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bitrateOptions = listOf(
                    Pair("Auto", VideoBitrate.AUTO),
                    Pair("4 Mbps", VideoBitrate.MBPS_4),
                    Pair("6 Mbps", VideoBitrate.MBPS_6),
                    Pair("8 Mbps", VideoBitrate.MBPS_8),
                    Pair("12 Mbps", VideoBitrate.MBPS_12),
                    Pair("16 Mbps", VideoBitrate.MBPS_16),
                    Pair("20 Mbps", VideoBitrate.MBPS_20)
                )
                CompactDropdownSelector(
                    label = "BITRATE",
                    currentText = selectedBitrate.label,
                    items = bitrateOptions.map { it.first to it.second },
                    selectedItem = selectedBitrate,
                    onItemSelected = onBitrateSelected,
                    modifier = Modifier.weight(1f)
                )

                val fpsOptions = listOf(
                    Pair("20 FPS", 20),
                    Pair("30 FPS", 30),
                    Pair("40 FPS", 40),
                    Pair("60 FPS", 60)
                )
                CompactDropdownSelector(
                    label = "FPS (FRAME RATE)",
                    currentText = "$selectedFps FPS",
                    items = fpsOptions,
                    selectedItem = selectedFps,
                    onItemSelected = onFpsSelected,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: DPI + MENU SCALE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dpiOptions = listOf(
                    Pair("160 DPI", 160),
                    Pair("180 DPI", 180),
                    Pair("240 DPI", 240),
                    Pair("310 DPI", 310),
                    Pair("400 DPI", 400)
                )
                CompactDropdownSelector(
                    label = "DISPLAY DPI",
                    currentText = "$selectedDpi DPI",
                    items = dpiOptions,
                    selectedItem = selectedDpi,
                    onItemSelected = onDpiSelected,
                    modifier = Modifier.weight(1f)
                )

                val scaleOptions = listOf(
                    Pair("80%", 80),
                    Pair("100% (Default)", 100),
                    Pair("120%", 120),
                    Pair("140%", 140)
                )
                CompactDropdownSelector(
                    label = "MENU SCALE",
                    currentText = "$selectedMenuScale%",
                    items = scaleOptions,
                    selectedItem = selectedMenuScale,
                    onItemSelected = onMenuScaleSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun <T> CompactDropdownSelector(
    label: String,
    currentText: String,
    items: List<Pair<String, T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBlack)
                .border(1.dp, if (expanded) TeslaAmber else ObsidianBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeslaAmber,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            ) {
                items.forEach { (text, value) ->
                    val isSelected = value == selectedItem
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TeslaAmber else TextPrimary
                            )
                        },
                        onClick = {
                            onItemSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
