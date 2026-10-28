package com.tesladroid.core.model

enum class VideoBitrate(
    val mbit: Int,
    val label: String
) {
    AUTO(0, "Auto"),
    MBPS_4(4, "4 Mbps"),
    MBPS_6(6, "6 Mbps"),
    MBPS_8(8, "8 Mbps"),
    MBPS_12(12, "12 Mbps"),
    MBPS_16(16, "16 Mbps"),
    MBPS_20(20, "20 Mbps");

    val mbps: Int
        get() = mbit
}

enum class AudioSettingMode(
    val id: String,
    val label: String,
    val description: String
) {
    TESLA_BT("bt", "Tesla BT", "Direct Bluetooth to car speakers"),
    WEB_OPUS("opus", "Web Opus", "Compressed Opus 64 kbps"),
    WEB_PCM("pcm", "Web PCM", "Lossless uncompressed PCM"),
    OFF("off", "Off", "Audio disabled")
}
