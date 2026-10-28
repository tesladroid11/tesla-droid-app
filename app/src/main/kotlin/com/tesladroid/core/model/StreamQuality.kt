package com.tesladroid.core.model

enum class StreamQuality(
    val label: String,
    val resolutionLabel: String,
    val width: Int,
    val height: Int,
    val bitrateMbps: Int,
    val targetFps: Int
) {
    HD_720P("720p", "1280x720", 1280, 720, 4, 30),
    FHD_1080P("1080p", "1920x1080", 1920, 1080, 8, 60),
    ULTRA("Ultra", "2400x1350", 2400, 1350, 16, 60)
}
