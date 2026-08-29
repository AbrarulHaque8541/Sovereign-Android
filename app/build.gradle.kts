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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/license.txt",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
    }
}

dependencies {
    val coreKtx = "androidx.core:core-ktx:1.12.0"
    val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
    val windowManager = "androidx.window:window:1.2.0"
    val materialComponents = "com.google.android.material:material:1.11.0"
    val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
    val okhttp = "com.squareup.okhttp3:okhttp:4.12.0"
    val kotlinxSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0"
    val datastore = "androidx.datastore:datastore-preferences:1.0.0"
    val media3 = "androidx.media3:media3-exoplayer:1.1.1"
    val media3Session = "androidx.media3:media3-session:1.1.1"
    val securityCrypto = "androidx.security:security-crypto:1.1.0-alpha06"
    val biometric = "androidx.biometric:biometric:1.2.0-alpha05"
    val workRuntime = "androidx.work:work-runtime-ktx:2.9.0"
    val okioVersion = "3.5.0"

    implementation(coreKtx)
    implementation(lifecycleRuntime)
    implementation(lifecycleViewModel)
    implementation(windowManager)
    implementation(materialComponents)
    implementation(kotlinCoroutines)
    implementation(okhttp)
    implementation(kotlinxSerialization)
    implementation(datastore)
    implementation(media3)
    implementation(media3Session)
    implementation(securityCrypto)
    implementation(biometric)
    implementation(workRuntime)

    implementation("com.squareup.okio:okio:$okioVersion")
    implementation("com.squareup.okio:okio-jvm:$okioVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.squareup.okio:okio-jvm:$okioVersion")
}