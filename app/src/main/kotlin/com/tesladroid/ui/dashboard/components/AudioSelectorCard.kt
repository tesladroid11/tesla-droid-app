package com.tesladroid.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary

@Composable
fun AudioSelectorCard(
    isAudioPcm: Boolean,
    onAudioModeChanged: (Boolean) -> Unit,
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
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = TeslaAmber,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VEHICLE AUDIO QUALITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBlack)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Option 1: Uncompressed (PCM)
                AudioOptionButton(
                    title = "Uncompressed",
                    subtitle = "Lossless Quality",
                    icon = Icons.Default.HighQuality,
                    isSelected = isAudioPcm,
                    onClick = { onAudioModeChanged(true) },
                    modifier = Modifier.weight(1f)
                )

                // Option 2: Compressed (Opus)
                AudioOptionButton(
                    title = "Compressed",
                    subtitle = "Bandwidth Saver",
                    icon = Icons.Default.Wifi,
                    isSelected = !isAudioPcm,
                    onClick = { onAudioModeChanged(false) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AudioOptionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) TeslaAmber else Color.Transparent,
        animationSpec = tween(200),
        label = "audioBg"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isSelected) ObsidianBlack else TextPrimary,
        animationSpec = tween(200),
        label = "audioTitle"
    )
    val subColor by animateColorAsState(
        targetValue = if (isSelected) ObsidianBlack.copy(alpha = 0.75f) else TextMuted,
        animationSpec = tween(200),
        label = "audioSub"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = titleColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = subColor,
                maxLines = 1
            )
        }
    }
}
