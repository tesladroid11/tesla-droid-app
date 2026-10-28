package com.tesladroid.core.model

enum class AudioMode(
    val label: String,
    val description: String
) {
    TESLA_BT("Tesla BT", "Direct Bluetooth audio to vehicle sound system"),
    WEB_AUDIO("Web Audio", "Capture and stream audio through Tesla browser"),
    OFF("Off", "Screen streaming without system audio routing")
}
