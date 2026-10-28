package com.tesladroid.ui.dashboard.components

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.tesladroid.theme.ObsidianBlack
import com.tesladroid.theme.ObsidianBorder
import com.tesladroid.theme.ObsidianSurface
import com.tesladroid.theme.ObsidianSurfaceHighlight
import com.tesladroid.theme.TeslaAmber
import com.tesladroid.theme.TeslaEmerald
import com.tesladroid.theme.TextMuted
import com.tesladroid.theme.TextPrimary
import com.tesladroid.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val componentName: String,
    val label: String,
    val icon: Drawable? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
    sheetState: SheetState,
    currentSelectedPackage: String = "",
    isMultiSelect: Boolean = false,
    selectedPackages: Set<String> = emptySet(),
    title: String = if (isMultiSelect) "TESLA MENU APPS" else "SELECT APPLICATION",
    subtitle: String = if (isMultiSelect) "Select apps to appear in the Tesla browser overlay menu" else "Select app for automatic launch or quick access",
    onAppSelected: (InstalledAppItem?) -> Unit = {},
    onMultiSelectSaved: (Set<String>) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentMultiSelection by remember(selectedPackages) { mutableStateOf(selectedPackages.toSet()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val list = resolveInfos.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                val label = ri.loadLabel(pm).toString()
                val icon = try { ri.loadIcon(pm) } catch (e: Exception) { null }
                val comp = "$pkg/${ri.activityInfo.name}"
                InstalledAppItem(
                    packageName = pkg,
                    componentName = comp,
                    label = label,
                    icon = icon
                )
            }.sortedBy { it.label.lowercase() }

            appsList = list
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, appsList) {
        if (searchQuery.isBlank()) {
            appsList
        } else {
            appsList.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ObsidianBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search application...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TeslaAmber,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeslaAmber,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedContainerColor = ObsidianBlack,
                    unfocusedContainerColor = ObsidianBlack,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TeslaAmber, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isMultiSelect) {
                        // Option to clear / disable (Single select only)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (currentSelectedPackage.isEmpty()) ObsidianSurfaceHighlight else ObsidianBlack)
                                    .border(1.dp, if (currentSelectedPackage.isEmpty()) TeslaAmber else ObsidianBorder, RoundedCornerShape(12.dp))
                                    .clickable { onAppSelected(null) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TextMuted.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "None (Disable autorun)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Do not launch any app automatically",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                if (currentSelectedPackage.isEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TeslaEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(filteredApps, key = { it.componentName }) { app ->
                        if (isMultiSelect) {
                            val isChecked = currentMultiSelection.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChecked) ObsidianSurfaceHighlight else ObsidianBlack)
                                    .border(1.dp, if (isChecked) TeslaAmber else ObsidianBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentMultiSelection = if (isChecked) {
                                            currentMultiSelection - app.packageName
                                        } else {
                                            currentMultiSelection + app.packageName
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TeslaAmber.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.take(1),
                                            fontWeight = FontWeight.Bold,
                                            color = TeslaAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isChecked) TeslaAmber else TextPrimary
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        currentMultiSelection = if (checked) {
                                            currentMultiSelection + app.packageName
                                        } else {
                                            currentMultiSelection - app.packageName
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = TeslaAmber,
                                        checkmarkColor = ObsidianBlack,
                                        uncheckedColor = TextMuted
                                    )
                                )
                            }
                        } else {
                            val isSelected = currentSelectedPackage.isNotEmpty() &&
                                    (currentSelectedPackage == app.packageName || currentSelectedPackage.startsWith(app.packageName))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ObsidianSurfaceHighlight else ObsidianBlack)
                                    .border(1.dp, if (isSelected) TeslaAmber else ObsidianBorder, RoundedCornerShape(12.dp))
                                    .clickable { onAppSelected(app) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TeslaAmber.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.take(1),
                                            fontWeight = FontWeight.Bold,
                                            color = TeslaAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) TeslaAmber else TextPrimary
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TeslaEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isMultiSelect) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onMultiSelectSaved(currentMultiSelection)
                            onDismiss()
                        },
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
                            text = "SAVE (${currentMultiSelection.size} SELECTED)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
