plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sovereign.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sovereign.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }

    tasks.withType<com.android.build.gradle.internal.tasks.StripDebugSymbolsTask> {
        enabled = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        )
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")
    val coreKtx = "androidx.core:core-ktx:1.12.0"
    val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
    val activityCompose = "androidx.activity:activity-compose:1.8.2"
    val windowManager = "androidx.window:window:1.2.0"
    val material3 = "androidx.compose.material3:material3:1.2.1"
    val materialComponents = "com.google.android.material:material:1.11.0"
    val composeFoundation = "androidx.compose.foundation:foundation:1.6.1"
    val composeUI = "androidx.compose.ui:ui:1.6.1"
    val composeUITooling = "androidx.compose.ui:ui-tooling:1.6.1"
    val composeUIToolingPreview = "androidx.compose.ui:ui-tooling-preview:1.6.1"
    val kotlinxCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    val okhttp = "com.squareup.okhttp3:okhttp:4.12.0"
    val kotlinxSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0"
    val datastore = "androidx.datastore:datastore-preferences:1.0.0"
    val navigationCompose = "androidx.navigation:navigation-compose:2.7.6"
    val media3 = "androidx.media3:media3-exoplayer:1.1.1"
    val media3Session = "androidx.media3:media3-session:1.1.1"
    val securityCrypto = "androidx.security:security-crypto:1.1.0-alpha06"
    val biometric = "androidx.biometric:biometric:1.2.0-alpha05"
    val workRuntime = "androidx.work:work-runtime-ktx:2.9.0"
    val okioVersion = "3.5.0"

    implementation(composeBom)
    implementation(coreKtx)
    implementation(lifecycleRuntime)
    implementation(lifecycleViewModel)
    implementation(activityCompose)
    implementation(windowManager)
    implementation(material3)
    implementation(materialComponents)
    implementation(composeFoundation)
    implementation(composeUI)
    implementation(composeUITooling)
    implementation(composeUIToolingPreview)
    implementation(kotlinxCoroutines)
    implementation(okhttp)
    implementation(kotlinxSerialization)
    implementation(datastore)
    implementation(navigationCompose)
    implementation(media3)
    implementation(media3Session)
    implementation(securityCrypto)
    implementation(biometric)
    implementation(workRuntime)

    // Okio - Zero-copy byte buffers and efficient I/O
    implementation("com.squareup.okio:okio:$okioVersion")
    implementation("com.squareup.okio:okio-jvm:$okioVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
}