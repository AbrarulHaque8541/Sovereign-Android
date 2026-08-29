# Sovereign Android Application
## Privacy-First, Offline-Capable Android App with Universal Capture, AI Models, and OTA Updates

### 📱 Current Status
**Build Architecture**: Native Android (Jetpack Compose stripped due to ARM64 ICE)  
**Debug APK**: Generates on x86_64; ARM64 PRoot Ubuntu requires native rebuild (see BUILD_LOGS.md)  
**Minimum SDK**: 24  
**Target SDK**: 34  

### ✅ Features Implemented
- **Universal Capture**: HEVC/H.265 + Opus screen+audio capture via MediaProjection
- **ADB/Fastboot Tools**: USB debugging and fastboot protocol bridging
- **OTA Updates**: On-demand model station with update checking
- **Extreme Storage**: GZIP compression cache (50MB max, auto-prune/flush)
- **8 Dynamic Themes**: Aurora, Cyber, Sunset, Ocean, Velvet, Forest, Candy, Mono
- **Background Services**: Local sync server on :8000; screen capture foreground service
- **No Shizuku/Bugjaeger**: All tools work without external dependencies

### 📂 Project Structure
```
app/src/main/java/com/sovereign/app/
├── MainActivity.kt          # Main entry point (Native Android)
├── SovereignApplication.kt  # App init; SharedPreferences; CoroutineScope
├── CaptureService.kt        # HEVC/Opus capture service
├── LocalSyncServer.kt       # Local sync HTTP server
├── LocalSyncService.kt      # Sync service
├── ModelManager.kt          # AI model management
├── MediaProjectionService.kt# Projection management
├── OverlayPermissionActivity.kt # Draw-overlays permission
├── UpdateInstallActivity.kt # OTA update installation
├── tools/
│   ├── ADBControlHubScreen.kt   # ADB/Fastboot UI
│   ├── FastbootProtocolBridge.kt# Fastboot USB
│   ├── ScriptRunnerUtility.kt   # Script execution
│   └── NativeSystemServiceEngine.kt # ADB/shell engine
├── updater/
│   ├── OTAUpdateManager.kt   # Update checking
│   └── UpdateInstallActivity.kt# Update installation
└── ui/
    ├── UniversalCaptureScreen.kt  # (Compose → Native XML migrated)
    └── theme/ThemeManager.kt     # 8 themes; SharedPreferences

app/src/main/res/
├─ layout/activity_main.xml    # Main UI layout (Native XML)
├─ values/themes.xml           # 8 theme configurations
└─ values/colors.xml           # Color resources

app/build.gradle.kts           # Build config (AGP 8.6.0, Kotlin 1.9.20)
gradle.properties              # aapt2 override
settings.gradle.kts            # Project inclusion
```

### 🛠️ Build Requirements
- **JDK**: 17 (OpenJDK 17.0.13)
- **Gradle**: 8.7
- **AGP**: 8.6.0
- **Android SDK**: /opt/android-sdk
- **AAPT2 Override**: `/usr/lib/android-sdk/build-tools/29.0.3/aapt2`

### 📱 Supported Architectures
- **x86_64**: Full build with all features; debug APK generates at `app/build/outputs/apk/debug/app-debug.apk`
- **ARM64 PRoot Ubuntu**: Native Android build works after Compose stripping (see BUILD_LOGS.md for ICE root cause)

### 📚 Documentation
- `BUILD_LOGS.md` - Full failure timeline, every error fix, root cause analysis
- `ARCHITECTURE_DECISIONS.md` - Why Compose→Native Android migration was necessary
- `CHANGELOG.md` - All changes summary with version-like detail
- `PROJECT_SUMMARY.md` - Phase-by-phase feature implementation log

### 🚀 Quick Start (x86_64)
```bash
# From /root
./gradlew clean assembleDebug --no-daemon
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 🛠️ Development
```bash
# Clean build
./gradlew clean assembleDebug --no-daemon

# View build logs
./gradlew assembleDebug --stacktrace 2>&1 | tail -50

# Check APK output
ls app/build/outputs/apk/debug/
```

### 📄 License
Privacy-first, offline-capable Android application. No Shizuku. No Bugjaeger.

---
*For full build debugging history, see BUILD_LOGS.md. For architecture decisions, see ARCHITECTURE_DECISIONS.md.*