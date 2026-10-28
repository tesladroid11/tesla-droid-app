package com.tesladroid.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import rikka.shizuku.Shizuku

object SystemSettingsHelper {

    fun isDevSettingsEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "development_settings_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun isAdbWifiEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api") != null
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun checkShizukuPermission(context: Context): Boolean {
        return try {
            if (isShizukuRunning()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                context.checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun requestShizukuPermission() {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(4242)
            }
        } catch (e: Throwable) {
            // Ignore if Shizuku not active
        }
    }

    fun autoGrantPermissionsViaShizuku(context: Context) {
        if (!isShizukuRunning() || !checkShizukuPermission(context)) return
        try {
            com.tesladroid.engine.ShizukuBridge.executeCommand(context, "pm grant ${context.packageName} android.permission.BLUETOOTH_CONNECT")
            com.tesladroid.engine.ShizukuBridge.executeCommand(context, "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        } catch (_: Throwable) {}
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open battery optimization settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please open notification settings manually", Toast.LENGTH_SHORT).show()
        }
    }

    fun isLicenseActive(context: Context): Boolean {
        return AppPreferences(context).isLicenseActive
    }

    fun activateLicense(context: Context) {
        AppPreferences(context).activateLicense()
        Toast.makeText(context, "TeslaDroid Engine is ready and activated!", Toast.LENGTH_SHORT).show()
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch application $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    fun openShizuku(context: Context) {
        val shizukuPackage = "moe.shizuku.privileged.api"
        if (isShizukuInstalled(context)) {
            launchApp(context, shizukuPackage)
        } else {
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(playStoreIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Please install Shizuku from shizuku.rikka.app", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openDevSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please open Developer Options manually", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAdbWifi(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please enable Wireless Debugging in Developer Options", Toast.LENGTH_SHORT).show()
        }
    }
}
