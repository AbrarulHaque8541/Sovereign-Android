# Keep Room entities and DAOs
-keep class com.sovereign.data.db.** { *; }

# Keep VaultManager
-keep class com.sovereign.data.security.VaultManager { *; }

# Keep Repositories
-keep class com.sovereign.data.repository.** { *; }

# Keep Services
-keep class com.sovereign.app.CaptureService { *; }
-keep class com.sovereign.app.LocalSyncService { *; }
-keep class com.sovereign.app.LocalSyncServer { *; }
-keep class com.sovereign.app.MediaProjectionService { *; }
-keep class com.sovereign.app.OverlayPermissionActivity { *; }

# Keep Application
-keep class com.sovereign.app.SovereignApplication { *; }

# Keep MainActivity
-keep class com.sovereign.app.MainActivity { *; }

# Keep Serialization
-keep class kotlinx.serialization.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Room
-keep class androidx.room.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }

# Keep Security Crypto
-keep class androidx.security.crypto.** { *; }

# Keep Biometric
-keep class androidx.biometric.** { *; }
