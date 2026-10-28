package com.tesladroid.ui.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesladroid.R
import com.tesladroid.core.model.ConnectionState
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.ObsidianSurfaceHighlight
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TeslaAmberGlow
import com.tesladroid.theme.TeslaCrimson
import com.tesladroid.theme.TeslaCrimsonGlow
import com.tesladroid.theme.TeslaEmerald
import com.tesladroid.theme.TeslaEmeraldGlow
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary

@Composable
fun HeroStatusCard(
    connectionState: ConnectionState,
    autoStart: Boolean,
    onAutoStartChanged: (Boolean) -> Unit,
    autoHotspot: Boolean = true,
    onAutoHotspotChanged: (Boolean) -> Unit = {},
    onDimScreenClick: () -> Unit,
    onPowerButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStreaming = connectionState == ConnectionState.STREAMING || connectionState == ConnectionState.CAR_CONNECTED
    val isCarConnected = connectionState == ConnectionState.CAR_CONNECTED

    val pulseScale: Float
    val pulseAlpha: Float
    if (isStreaming) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.60f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        pulseScale = scale
        pulseAlpha = alpha
    } else {
        pulseScale = 1.0f
        pulseAlpha = 0.0f
    }

    val activeGlowColor = when {
        isCarConnected -> TeslaEmerald
        isStreaming -> TeslaAmber
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ObsidianSurfaceHighlight,
                        ObsidianSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isStreaming) activeGlowColor.copy(alpha = 0.6f) else ObsidianBorder,
                        ObsidianBorder.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Vehicle Connection State if active
            if (isCarConnected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(TeslaEmeraldGlow)
                        .border(1.dp, TeslaEmerald.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TeslaEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Tesla",
                        tint = TeslaEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "TESLA CONNECTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TeslaEmerald
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Center: Big Power / Broadcast Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(136.dp)
            ) {
                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(134.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(activeGlowColor.copy(alpha = pulseAlpha))
                    )
                }

                val buttonBrush = when {
                    isStreaming -> Brush.radialGradient(
                        colors = listOf(
                            TeslaCrimson,
                            Color(0xFFB71C1C)
                        )
                    )
                    else -> Brush.radialGradient(
                        colors = listOf(
                            TeslaAmber,
                            Color(0xFFC77700)
                        )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(106.dp)
                        .clip(CircleShape)
                        .shadow(
                            elevation = if (isStreaming) 16.dp else 8.dp,
                            shape = CircleShape,
                            ambientColor = if (isStreaming) TeslaCrimsonGlow else TeslaAmberGlow,
                            spotColor = if (isStreaming) TeslaCrimson else TeslaAmber
                        )
                        .background(buttonBrush)
                        .border(
                            width = 2.dp,
                            color = if (isStreaming) Color(0xFFFF8A80) else Color(0xFFFFD580),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onPowerButtonClick
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isCarConnected -> Icons.Default.CastConnected
                                isStreaming -> Icons.Default.Stop
                                else -> Icons.Default.Cast
                            },
                            contentDescription = "Power",
                            tint = if (isStreaming) Color.White else ObsidianBlack,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isStreaming) "STOP" else "START",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (isStreaming) Color.White else ObsidianBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Title
            Text(
                text = when (connectionState) {
                    ConnectionState.IDLE -> "READY TO STREAM"
                    ConnectionState.PREPARING -> "INITIALIZING..."
                    ConnectionState.STREAMING -> "STREAMING ACTIVE"
                    ConnectionState.CAR_CONNECTED -> "ACTIVE SESSION IN TESLA BROWSER"
                    ConnectionState.ERROR -> "CONNECTION ERROR"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = when (connectionState) {
                    ConnectionState.CAR_CONNECTED -> TeslaEmerald
                    ConnectionState.STREAMING -> TeslaAmber
                    ConnectionState.ERROR -> TeslaCrimson
                    else -> TextPrimary
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row: Auto-start (Left 50%) and Dim Screen (Right 50%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left 50%: Auto-start
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBlack.copy(alpha = 0.6f))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        .clickable { onAutoStartChanged(!autoStart) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Auto-start",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoStart) TeslaAmber else TextPrimary
                            )
                            Text(
                                text = "On connect",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = autoStart,
                            onCheckedChange = onAutoStartChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = TeslaAmber,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ObsidianSurface
                            ),
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                }

                // Right 50%: Dim Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBlack.copy(alpha = 0.6f))
                        .border(1.dp, TeslaAmber.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = TeslaAmber),
                            onClick = onDimScreenClick
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TeslaAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_dim),
                                contentDescription = "Dim Screen",
                                tint = TeslaAmber,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Dim Screen",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Power saving",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row: Auto Hotspot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBlack.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        if (autoHotspot) TeslaAmber.copy(alpha = 0.35f) else ObsidianBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onAutoHotspotChanged(!autoHotspot) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (autoHotspot) TeslaAmber.copy(alpha = 0.15f) else ObsidianSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Hotspot",
                                tint = if (autoHotspot) TeslaAmber else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Auto-Stop Hotspot",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoHotspot) TeslaAmber else TextPrimary
                            )
                            Text(
                                text = if (autoHotspot) "Turn off hotspot when streaming ends" else "Keep hotspot running after stop",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = autoHotspot,
                        onCheckedChange = onAutoHotspotChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = TeslaAmber,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurface
                        ),
                        modifier = Modifier.scale(0.75f)
                    )
                }
            }
        }
    }
}
