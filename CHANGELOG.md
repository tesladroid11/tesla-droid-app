# Changelog

All notable changes to **TeslaDroid** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-19

### Added
- **Modern Jetpack Compose UI**: Complete Material 3 dashboard engineered with obsidian titanium styling, high contrast indicators, and driver-focused ergonomics.
- **Independent High-Performance Engine Architecture**:
  - Native CIO server operating on port 8443 (HTTPS + WSS) with integrated TLS termination.
  - Low-latency virtual display streaming pipeline with MediaCodec hardware encoding (H.264 / HEVC / H.265).
  - Bi-directional touch and multi-touch input injection via Shizuku privileged system bridge (`/dev/input`).
- **Smart Connectivity & Automation**:
  - **Bluetooth Trigger**: Automatic stream launch when connected to vehicle Bluetooth, with automatic 30-second shutdown timer upon disconnection.
  - **Wi-Fi Network Trigger**: Automatic stream launch when connecting to designated Wi-Fi networks (secondary phone hotspot, in-car router, etc.) and auto-stop on disconnect.
  - **Automated Hotspot Management**: Auto-starts phone's mobile hotspot on stream launch and disables it on session completion.
  - **Short URL Redirection**: 6-character short codes for instant typing in Tesla's built-in Chromium browser.
- **Display & Audio Customization**:
  - Configurable bitrate (Auto, 4–20 Mbps), frame rate (20–60 FPS), and virtual display density (160–400 DPI).
  - Uncompressed PCM audio mode and low-bandwidth Opus audio compression.
  - Left-hand drive sidebar docking (Strip left) and hardware orientation lock (Pin rotation).
  - Integrated odometer, trip telemetry, and speed overlay.
- **Navigation & Anti-Jamming Cybersecurity**:
  - **GPS Shield (Anti-Jamming & Spoofing Protection)**: Advanced cybersecurity navigation feature protecting navigation applications (Waze, Google Maps) against satellite anomalies, RF jamming, and spoofing attacks through seamless cellular and Wi-Fi fused positioning fallback.
- **Multimodal AI In-Cabin Assistant**:
  - Google Gemini live multimodal voice assistance with configurable voice profiles.
  - Wake-word detection ("Hey Tesla") and animated edge glow UI effects.
  - In-browser interactive floating assistant button.
- **Battery & Display Protection**:
  - Configurable screen auto-dimming to preserve OLED panels and minimize thermal buildup while streaming.
