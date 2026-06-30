plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tech.phantom.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "tech.phantom.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // Optional release signing: active only when PHANTOM_KEYSTORE points at a
    // real keystore (e.g. for a shareable release APK). Otherwise the release
    // build is simply unsigned and CI's debug build is unaffected.
    val keystorePath = System.getenv("PHANTOM_KEYSTORE")
    signingConfigs {
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("PHANTOM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PHANTOM_KEY_ALIAS")
                keyPassword = System.getenv("PHANTOM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    // On-device LAM loop (Phase 4): HTTP to local Ollama + the PC gate.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // In-app QR pairing scanner: CameraX + ML Kit barcode scanning.
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    // Embedded on-device LLM (Phase 6): MediaPipe LLM Inference (vision).
    implementation("com.google.mediapipe:tasks-genai:0.10.24")
    // Provides com.google.mediapipe.framework.image.{MPImage,BitmapImageBuilder}
    // used to pass screenshots into the vision model.
    implementation("com.google.mediapipe:tasks-vision:0.10.24")
}
