package com.tesladroid.core.model

data class PermissionsState(
    val devSettings: Boolean = false,
    val adbWifi: Boolean = false,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuPermission: Boolean = false,
    val batteryOptimization: Boolean = false,
    val notifications: Boolean = false,
    val licenseActive: Boolean = false,
    val screenCapture: Boolean = false
) {
    val allCrucialGranted: Boolean
        get() = (shizukuRunning && shizukuPermission && batteryOptimization) || (screenCapture && batteryOptimization)
}
