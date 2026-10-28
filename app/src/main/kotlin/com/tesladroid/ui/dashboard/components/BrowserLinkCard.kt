package com.tesladroid.ui.dashboard.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextSecondary

@Composable
fun BrowserLinkCard(
    url: String,
    shortUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TeslaAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        tint = TeslaAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (!shortUrl.isNullOrEmpty()) "TYPE IN TESLA BROWSER" else "TESLA BROWSER URL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!shortUrl.isNullOrEmpty()) {
                // Short URL Display Box (High contrast, prominent, tap-to-copy)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBlack)
                        .border(1.dp, TeslaAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Tesla Short URL", shortUrl))
                            Toast.makeText(context, "Short URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = shortUrl,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        lineHeight = 22.sp,
                        color = TeslaAmber
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Full URL Display Box (Bookmark / Fallback)
                Text(
                    text = "Full address — for bookmarking:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBlack.copy(alpha = 0.6f))
                        .border(1.dp, ObsidianBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Tesla Full URL", url))
                            Toast.makeText(context, "Full URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = url,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
            } else {
                // Full URL Display Box (High contrast, clearly visible, tap-to-copy)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBlack)
                        .border(1.dp, ObsidianBorder.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Tesla URL", url)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = url,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        lineHeight = 20.sp,
                        color = TeslaAmber
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Open this address in the browser on your Tesla center screen",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
