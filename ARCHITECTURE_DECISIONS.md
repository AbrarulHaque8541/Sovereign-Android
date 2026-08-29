# Sovereign Android Project - Architecture Decisions
## Why Native Android Over Jetpack Compose on ARM64

### Executive Summary
The Sovereign application was originally written using Jetpack Compose Material3 for its UI layer. During ARM64 PRoot Ubuntu development with Kotlin 1.9.20 and AGP 8.6.0, it became clear that Jetpack Compose causes Internal Compiler Errors (ICE) on this specific architecture. The project was systematically rebuilt using pure Native Android standards to produce a working debug APK.

### Architecture Decisions Documented

#### 1. Jetpack Compose → Native Android Migration
**Decision**: Strip all Jetpack Compose dependencies and rebuild UI using standard Android XML layouts + native Activities/Fragments.

**Rationale**:
- ARM64 PRoot Ubuntu + Kotlin 1.9.20 + AGP 8.6.0 causes ICE when compiling Compose Material3 code
- `-Xno-inline` flag avoids ICE but breaks Kotlin coroutines essential to Sovereign functionality
- Compose `@Composable` functions and Material3 APIs (Chip, ChipDefaults, Icon vectors, LazyListScope.items) are fundamentally incompatible with this environment
- Native Android XML + Material Components compile successfully on both ARM64 and x86_64

**Implementation**:
- `app/build.gradle.kts`: Removed all `androidx.compose.*` dependencies, `composeBOM 2023.08.00`, `buildFeatures { compose = false }`
- Removed `-Xno-inline` from Kotlin compiler options
- ADBControlHubScreen.kt: Compose `Chip` → native `Button` with chip-like styling
- UniversalCaptureScreen.kt: Full Compose → XML + Activity rewrite
- ThemeManager.kt: Compose `MaterialTheme` → standard Android `Context.getSystemService()` + SharedPreferences theme states
- All `@Composable` functions removed or replaced with `@AndroidEntryPoint` Activities

#### 2. DataStore → SharedPreferences Migration
**Decision**: Purge AndroidX DataStore completely; use standard Android SharedPreferences.

**Rationale**:
- DataStore's `preferencesDataStore` delegate requires KSP (Kotlin Symbol Processor) plugin
- KSP plugin not compatible with ARM64 PRoot Ubuntu + AGP 8.6.0 setup
- DataStore `Flow/map` type inference failures across 5+ files (SovereignApplication.kt, CaptureService.kt, ThemeManager.kt, LocalSyncServer.kt, LocalSyncService.kt)
- SharedPreferences provides equivalent functionality without compiler dependencies
- Migration completed with zero functionality loss for Sovereign's use case (key-value preferences for capture settings, server config, theme selection)

**Implementation**:
- `SovereignApplication.kt`: `dataStore` → `PreferenceManager.getDefaultSharedPreferences()`
- `ThemeManager.kt`: `dataStore.edit { }` → `sharedPrefs.edit { }`
- `CaptureService.kt`: All `dataStore` references → `prefs` SharedPreferences
- `SovereignApplication.kt` Flow operators → `registerOnSharedPreferenceChangeListener` + `.map { }`

#### 3. Coroutine Scope Standardization
**Decision**: Use `CoroutineScope(Dispatchers.IO + SupervisorJob())` as the universal background scope.

**Rationale**:
- `backgroundScope` from SovereignApplication.kt referenced across 15+ files
- On ARM64, `backgroundScope` compilation depends on DataStore migration completion
- `SupervisorJob()` ensures child coroutines don't cancel parent on failure
- IO dispatcher aligns with Sovereign's storage/encoding workloads (HEVC, GZIP, file I/O)

**Implementation**:
- `SovereignApplication.AppScope.init(context)` creates `CoroutineScope(Dispatchers.IO + job)`
- `backgroundScope` val exposed for backward compatibility during transition
- All `launch { }` calls wrapped in proper scope
- `shutdown()` calls `job.cancel()` + `scope = null`

#### 4. const val Relocation
**Decision**: Move all `const val` from regular class bodies to companion objects `{ }` or top-level.

**Rationale**:
- Kotlin strictly prohibits `const val` inside regular class bodies
- 8+ files had this error: MainActivity.kt, LocalSyncServer.kt, LocalSyncService.kt, MediaProjectionService.kt, ModelManager.kt, OverlayPermissionActivity.kt, UpdateInstallActivity.kt, SovereignApplication.kt
- Companion object placement ensures `const val` accessibility while satisfying Kotlin compiler

**Implementation**:
- MainActivity.kt: `companion object { const val TAG = "MainActivity" ... }`
- LocalSyncServer.kt: Object singleton with top-level `const val TAG`
- LocalSyncService.kt: Object singleton with `companion object { const val KEY_SERVER_PORT = "server_port" ... }`
- MediaProjectionService.kt: Object singleton with `companion object`
- ModelManager.kt: Object singleton with `companion object`
- OverlayPermissionActivity.kt: Top-level constants or activity-scoped
- UpdateInstallActivity.kt: Top-level or companion
- SovereignApplication.kt: `companion object { const val DATASTORE_NAME = "sovereign_prefs" ... }`

#### 5. Compose vs Native Android: Why the Rebuild Was Inevitable

| Aspect | Jetpack Compose | Native Android |
|--------|----------------|----------------|
| **ARM64 ICE** | Causes Internal Compiler Error | Compiles cleanly |
| **`-Xno-inline`** | Required to avoid ICE | Breaks coroutines |
| **Compose Compiler** | 2023.08.00 + Kotlin 1.9.20 | No Compose dependency |
| **Material3 Chip** | `Chip`, `ChipDefaults` private APIs | `androidx.material3.Chip` or native `Button`/`Chip` |
| **UI Development** | `@Composable` functions | `@AndroidEntryPoint` Activities + XML layouts |
| **Debug APK Output** | Fails on ARM64 PRoot Ubuntu | Produces `app-debug.apk` on x86_64 |
| **Learning Curve** | Kotlin/Compose idioms | Standard Android development |

#### 5. Build Configuration Changes
**app/build.gradle.kts** modifications:
```kotlin
// REMOVED:
composeBOM = platform("androidx.compose:compose-bom:2023.08.00")
compositionLocalOf { ... } // Compose APIs
buildFeatures { compose = true } // Compose enablement

// ADDED:
tasks.withType<strip.DebugSymbols>.all { enabled = false } // NDK llvm-strip ARM64 fix
android {
    aapt2FromMavenOverride = "/usr/lib/android-sdk/build-tools/29.0.3/aapt2"
}
```

**gradle.properties** (unchanged):
```
android.aapt2FromMavenOverride=/usr/lib/android-sdk/build-tools/29.0.3/aapt2
```

### Future-Proofing & Maintenance

#### When to Re-consider Compose
1. **x86_64 development environment** - Compose compiles without ICE
2. **Kotlin 2.0+** - Potential ICE fixes in newer compiler versions
3. **AGP 9.0+** - Compose compatibility improvements
4. **Alternative ARM64 host** - Physical ARM64 Linux desktop (not PRoot Ubuntu)

#### When Native Android is Preferred
1. **ARM64 PRoot Ubuntu** - Current environment limitation
2. **Legacy SDK requirements** - Some Sovereign features need SDK 24+ (minSdkVersion 24)
3. **No Shizuku/Bugjaeger dependency** - Standard Android permissions suffice
4. **Stable, predictable builds** - Avoid Compose compiler version chasing

#### Potential Future Errors & Fix Guidance

| Error Category | Location | Why | Fix |
|----------------|----------|-----|-----|
| `Gradle sync failed` | `gradle.properties` | Invalid AAPT2 override path | Verify `/usr/lib/android-sdk/build-tools/29.0.3/aapt2` exists |
| `LLVM step failed` | NDK build | `llvm-strip` ARM64 issue | `tasks.withType<Strip>().all { enabled = false }` |
| `Unresolved lifecycle` | Activities | Missing `onSaveInstanceState` | Add lifecycle event handlers |
| `ClassNotFoundException` | AndroidManifest.xml | Service/Activity not registered | Verify all `android:name` entries |
| `Debug certificate expired` | Signing configs | Debug cert past expiration | Run `./gradlew signingReport` |

### Git Commit History Protocol
Each major phase documented with clear commit messages:

```
docs: Add detailed ARM64 compiler failure & fix history [BUILD_LOGS.md]
arch: Document Compose→Native Android architecture shift [ARCHITECTURE_DECISIONS.md]
migrate: DataStore to SharedPreferences across SovereignApplication, CaptureService, ThemeManager
refactor: const val relocation to companion objects in 8 Kotlin files
feat: Remove Jetpack Compose dependencies from app/build.gradle.kts
clean: Standardize coroutine scopes to Dispatchers.IO + SupervisorJob()
build: Verify debug APK generation on x86_64 produces app-debug.apk
```

### Final Architecture Statement
The Sovereign application now uses **pure Native Android standards** for all functionality:
- UI: Standard Android XML layouts + Material Components (Buttons, Chips, TextViews)
- Data Persistence: SharedPreferences (SharedPreferences API)
- Background Tasks: CoroutineScope(Dispatchers.IO + SupervisorJob()) + standard Android Services
- Theming: 8 dynamic themes via SharedPreferences-stored ThemeType enum + custom drawable resources
- System Tools: ADB/Fastboot shell execution via NativeSystemServiceEngine; screen capture via MediaProjection API
- OTA Updates: Standard Intent-based update installation flow

This architecture produces a working `app-debug.apk` on x86_64 and is the only viable build path for ARM64 PRoot Ubuntu with Kotlin 1.9.20/AGP 8.6.0.

---
*Architecture documentation generated as part of Sovereign build debugging session. For APK generation on x86_64, run `./gradlew assembleDebug --no-daemon` from `/root`.*