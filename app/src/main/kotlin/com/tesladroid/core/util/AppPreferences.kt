package com.tesladroid.core.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.tesladroid.core.engine.CoreEngineBridge

class AppPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tesladroid_prefs", Context.MODE_PRIVATE)
    private val tmPrefs: SharedPreferences = context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)

    private fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        tmPrefs.edit().putString(key, value).apply()
    }

    private fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        tmPrefs.edit().putInt(key, value).apply()
    }

    private fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        tmPrefs.edit().putBoolean(key, value).apply()
    }

    private fun putStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, HashSet(value)).apply()
        tmPrefs.edit().putStringSet(key, HashSet(value)).apply()
    }

    var autoStart: Boolean
        get() = prefs.getBoolean("auto_start", false)
        set(value) = putBoolean("auto_start", value)

    var autoLaunchPackage: String
        get() = prefs.getString("auto_launch", "") ?: ""
        set(value) = putString("auto_launch", value)

    var autoStopSeconds: Int
        get() = prefs.getInt("auto_stop_sec", 0)
        set(value) {
            putInt("auto_stop_sec", value)
            tmPrefs.edit().remove("auto_stop_min").apply()
        }

    var autoDimSeconds: Int
        get() = prefs.getInt("auto_dim_sec", 30)
        set(value) = putInt("auto_dim_sec", value)

    var carBtAddr: String
        get() = prefs.getString("car_bt_addr", "") ?: ""
        set(value) = putString("car_bt_addr", value)

    var carBtName: String
        get() = prefs.getString("car_bt_name", "") ?: ""
        set(value) = putString("car_bt_name", value)

    var lockDim: Boolean
        get() = prefs.getBoolean("lock_dim", true)
        set(value) = putBoolean("lock_dim", value)

    var powerReveal: Boolean
        get() = prefs.getBoolean("power_reveal", false)
        set(value) = putBoolean("power_reveal", value)

    var notifPopup: Int
        get() = prefs.getInt("notif_popup", 1)
        set(value) = putInt("notif_popup", value)

    var notifText: Boolean
        get() = prefs.getBoolean("notif_text", true)
        set(value) = putBoolean("notif_text", value)

    var notifOngoing: Boolean
        get() = prefs.getBoolean("notif_ongoing", true)
        set(value) = putBoolean("notif_ongoing", value)

    var muteNotif: Boolean
        get() = prefs.getBoolean("mute_notif", false)
        set(value) {
            putBoolean("mute_notif", value)
            tmPrefs.edit().putBoolean("mute_notif_sound", value).apply()
        }

    var shortCode: String
        get() = prefs.getString("short_code", "") ?: ""
        set(value) = putString("short_code", value)

    var videoBitrateMbps: Int
        get() = prefs.getInt("bitrate_mbit", 8)
        set(value) = putInt("bitrate_mbit", value)

    var fps: Int
        get() = prefs.getInt("fps", 60)
        set(value) = putInt("fps", value)

    var displayDpi: Int
        get() = prefs.getInt("display_dpi", 240)
        set(value) = putInt("display_dpi", value)

    var stripLeft: Boolean
        get() = prefs.getBoolean("strip_left", true)
        set(value) = putBoolean("strip_left", value)

    var pinRotation: Boolean
        get() = prefs.getBoolean("pin_rotation", true)
        set(value) = putBoolean("pin_rotation", value)

    var displayDecorations: Boolean
        get() = prefs.getBoolean("display_decorations", false)
        set(value) = putBoolean("display_decorations", value)

    var hideNavBar: Boolean
        get() = prefs.getBoolean("hide_nav_bar", false)
        set(value) = putBoolean("hide_nav_bar", value)

    var odometer: Boolean
        get() = prefs.getBoolean("odometer", false)
        set(value) = putBoolean("odometer", value)

    var carGpsNav: Boolean
        get() = prefs.getBoolean("car_gps_nav", false)
        set(value) = putBoolean("car_gps_nav", value)

    var audioMode: String
        get() = prefs.getString("audio_mode", "TESLA_BT") ?: "TESLA_BT"
        set(value) {
            putString("audio_mode", value)
            val pcm = (value != "WEB_OPUS")
            putBoolean("audio_pcm", pcm)
        }

    var audioPcm: Boolean
        get() = prefs.getBoolean("audio_pcm", true)
        set(value) = putBoolean("audio_pcm", value)

    var videoDelayMs: Int
        get() = prefs.getInt("video_delay_ms", 0)
        set(value) = putInt("video_delay_ms", value)

    var hotspot5Ghz: Boolean
        get() = prefs.getBoolean("hotspot_5ghz", true)
        set(value) = putBoolean("hotspot_5ghz", value)

    var autoHotspot: Boolean
        get() = prefs.getBoolean("auto_hotspot", true)
        set(value) = putBoolean("auto_hotspot", value)

    var googleAssistant: Boolean
        get() = prefs.getBoolean("google_assistant", true)
        set(value) = putBoolean("google_assistant", value)

    var geminiModel: String
        get() = prefs.getString("gemini_model", "gemini-2.5-flash-native-audio-preview-12-2025") ?: "gemini-2.5-flash-native-audio-preview-12-2025"
        set(value) = putString("gemini_model", value)

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = putString("gemini_api_key", value)

    var geminiVoice: String
        get() = prefs.getString("gemini_voice", "Puck") ?: "Puck"
        set(value) = putString("gemini_voice", value)

    var assistantGain: Int
        get() = prefs.getInt("assistant_gain", 2)
        set(value) {
            putInt("assistant_gain", value)
            putInt("grok_gain", value)
        }

    var assistantMic: Int
        get() = prefs.getInt("assistant_mic", 0)
        set(value) {
            putInt("assistant_mic", value)
            putInt("grok_mic", value)
        }

    var hudShown: Boolean
        get() = prefs.getBoolean("hud_shown", false)
        set(value) = putBoolean("hud_shown", value)

    var dontCloseOnStop: Boolean
        get() = prefs.getBoolean("dont_close_on_stop", false)
        set(value) = putBoolean("dont_close_on_stop", value)

    var glowSize: Int
        get() = prefs.getInt("glow_size", 2)
        set(value) = putInt("glow_size", value)

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake_word", true)
        set(value) {
            putBoolean("wake_word", value)
            try {
                tmPrefs.edit().putBoolean("wake_word", value).apply()
            } catch (_: Throwable) {}
        }

    var wakeThreshold: Int
        get() = prefs.getInt("wake_threshold_v2", 75)
        set(value) {
            putInt("wake_threshold_v2", value)
            try {
                context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)
                    .edit().putInt("wake_threshold_v2", value).apply()
            } catch (_: Throwable) {}
            try {
                val wwCls = Class.forName("com.invmaster.teslamirror.WakeWord")
                wwCls.getField("b").set(null, value.toFloat() / 100.0f)
            } catch (_: Throwable) {}
        }

    var voiceCues: Boolean
        get() = prefs.getBoolean("voice_cues", true)
        set(value) {
            putBoolean("voice_cues", value)
            putInt("cue_mode", if (value) 1 else 0)
        }

    var inCarAssistantButtonEnabled: Boolean
        get() = prefs.getBoolean("in_car_assistant_on", false)
        set(value) {
            putBoolean("in_car_assistant_on", value)
            putBoolean("grok_on", value)
        }

    var menuScale: Int
        get() = prefs.getInt("menu_scale", 100)
        set(value) = putInt("menu_scale", value)

    var phoneActions: Boolean
        get() = prefs.getBoolean("phone_actions", true)
        set(value) = putBoolean("phone_actions", value)

    var hiddenApps: Set<String>
        get() = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
        set(value) = putStringSet("hidden_apps", value)

    var keepPlayingApps: Set<String>
        get() {
            val set = prefs.getStringSet("keep_playing_apps", null)
            if (set != null && set.isNotEmpty()) {
                // Ensure default navigation apps are included if not explicitly removed
                val defaults = setOf("com.waze", "com.google.android.apps.maps")
                return if (set.contains("com.google.android.youtube") && !set.contains("com.waze")) set + defaults else set
            }
            return setOf(
                "com.waze",
                "com.google.android.apps.maps",
                "com.spotify.music",
                "com.google.android.youtube",
                "com.google.android.apps.youtube.music"
            )
        }
        set(value) {
            putStringSet("keep_playing_apps", value)
            try {
                context.getSharedPreferences("teslamirror", Context.MODE_PRIVATE)
                    .edit().putStringSet("keep_playing_apps", HashSet(value)).apply()
            } catch (_: Throwable) {}
        }

    var keepAliveApps: Set<String>
        get() = prefs.getStringSet("keep_alive_apps", setOf("com.neptunalarm.neptun_alarm_app", "ua.alertwidget")) ?: emptySet()
        set(value) = putStringSet("keep_alive_apps", value)

    var wifiAutoStart: Boolean
        get() = prefs.getBoolean("wifi_auto_start", false)
        set(value) = putBoolean("wifi_auto_start", value)

    var wifiAutoStartSsid: String
        get() = prefs.getString("wifi_auto_start_ssid", "") ?: ""
        set(value) = putString("wifi_auto_start_ssid", value)

    fun getAllLauncherPackages(): List<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            val fromIntent = context.packageManager.queryIntentActivities(intent, flags)
                .mapNotNull { it.activityInfo?.packageName }

            val fromInstalled = try {
                context.packageManager.getInstalledPackages(0).map { it.packageName }
            } catch (_: Throwable) {
                emptyList()
            }

            (fromIntent + fromInstalled).distinct()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    var menuApps: Set<String>
        get() {
            val raw = prefs.getString("menu_apps", null)
            if (raw != null) {
                return if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
            }
            return setOf(
                "com.waze",
                "com.google.android.apps.maps",
                "com.spotify.music"
            )
        }
        set(value) {
            prefs.edit().putString("menu_apps", value.joinToString(",")).apply()
            syncHiddenApps(value)
        }

    fun syncHiddenApps(selected: Set<String> = menuApps) {
        try {
            val allPkgs = getAllLauncherPackages()
            if (allPkgs.isNotEmpty()) {
                val hidden = allPkgs.filter { !selected.contains(it) }.toSet()
                putStringSet("hidden_apps", hidden)
                Log.i("AppPreferences", "Synced hidden_apps (${hidden.size} hidden, ${selected.size} visible in menu)")
            }
        } catch (e: Throwable) {
            Log.w("AppPreferences", "Failed to sync hidden apps: ${e.message}")
        }
    }

    fun syncAllToTeslaMirror() {
        try {
            val tm = tmPrefs.edit()
            tm.putBoolean("auto_start", autoStart)
            tm.putString("auto_launch", autoLaunchPackage)
            tm.putInt("auto_stop_sec", autoStopSeconds)
            tm.putInt("auto_dim_sec", autoDimSeconds)
            tm.putString("car_bt_addr", carBtAddr)
            tm.putString("car_bt_name", carBtName)
            tm.putBoolean("lock_dim", lockDim)
            tm.putBoolean("power_reveal", powerReveal)
            tm.putInt("notif_popup", notifPopup)
            tm.putBoolean("notif_text", notifText)
            tm.putBoolean("notif_ongoing", notifOngoing)
            tm.putBoolean("mute_notif", muteNotif)
            tm.putBoolean("mute_notif_sound", muteNotif)
            tm.putString("short_code", shortCode)
            tm.putInt("bitrate_mbit", videoBitrateMbps)
            tm.putInt("fps", fps)
            tm.putInt("display_dpi", displayDpi)
            tm.putBoolean("strip_left", stripLeft)
            tm.putBoolean("pin_rotation", pinRotation)
            tm.putBoolean("display_decorations", displayDecorations)
            tm.putBoolean("hide_nav_bar", hideNavBar)
            tm.putBoolean("odometer", odometer)
            tm.putBoolean("car_gps_nav", carGpsNav)
            tm.putBoolean("audio_pcm", audioPcm)
            tm.putInt("video_delay_ms", videoDelayMs)
            tm.putBoolean("hotspot_5ghz", hotspot5Ghz)
            tm.putBoolean("google_assistant", googleAssistant)
            tm.putString("gemini_model", geminiModel)
            tm.putString("gemini_api_key", geminiApiKey)
            tm.putString("gemini_voice", geminiVoice)
            tm.putInt("assistant_gain", assistantGain)
            tm.putInt("grok_gain", assistantGain)
            tm.putInt("assistant_mic", assistantMic)
            tm.putInt("grok_mic", assistantMic)
            tm.putInt("gemini_volume_diff", 0)
            tm.remove("gemini_volume")
            tm.putBoolean("hud_shown", hudShown)
            tm.putInt("glow_size", glowSize)
            tm.putBoolean("wake_word", wakeWordEnabled)
            tm.putBoolean("voice_cues", voiceCues)
            tm.putInt("cue_mode", if (voiceCues) 1 else 0)
            tm.putBoolean("in_car_assistant_on", inCarAssistantButtonEnabled)
            tm.putBoolean("grok_on", inCarAssistantButtonEnabled)
            tm.putInt("menu_scale", menuScale)
            tm.putBoolean("phone_actions", phoneActions)
            tm.putInt("wake_threshold_v2", wakeThreshold)
            tm.putStringSet("keep_playing_apps", HashSet(keepPlayingApps))
            tm.putStringSet("keep_alive_apps", HashSet(keepAliveApps))
            tm.putInt("res_scale", resScale)
            tm.putStringSet("app_scale", HashSet(appScale))
            tm.apply()

            syncHiddenApps()
            if (!prefs.getBoolean("dock_favorites_cleaned", false)) {
                putString("dock_favorites", "[]")
                prefs.edit().putBoolean("dock_favorites_cleaned", true).apply()
            }
            Log.i("AppPreferences", "All preferences synchronized into teslamirror SharedPreferences")
        } catch (e: Throwable) {
            Log.e("AppPreferences", "syncAllToTeslaMirror failed: ${e.message}")
        }
    }


    var resScale: Int
        get() = prefs.getInt("res_scale", 100)
        set(value) = putInt("res_scale", value)

    var appScale: Set<String>
        get() = prefs.getStringSet("app_scale", emptySet()) ?: emptySet()
        set(value) = putStringSet("app_scale", value)

    val isLicenseActive: Boolean
        get() = prefs.getBoolean("tesladroid_activated", true)

    fun activateLicense() {
        prefs.edit().putBoolean("tesladroid_activated", true).apply()
        tmPrefs.edit().putBoolean("tesladroid_activated", true).apply()
    }
}
