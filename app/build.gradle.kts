plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

// Versioning (Phase 8): CI in dono env vars ko set karke override kar deti hai
// (versionCode = GitHub Actions run number, versionName = git tag jaise "v1.2.0" -> "1.2.0").
// Local build me (Android Studio / seedha `gradle` command) yeh env vars set nahi hote,
// isliye neeche wali default values (versionCode = 1, versionName = "1.0") use ho jaati
// hain — koi extra setup ki zaroorat nahi local dev ke liye.
val appVersionCode = (System.getenv("APP_VERSION_CODE") ?: "1").toInt()
val appVersionName = System.getenv("APP_VERSION_NAME") ?: "1.0"

android {
    namespace = "com.eve.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eve.app"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // Fixed debug keystore (repo me committed) -> SHA-1 hamesha same rahega,
        // isliye GitHub Actions ke debug APK me bhi Google Sign-in chalega.
        getByName("debug") {
            storeFile = file("eve-debug.keystore")
            storePassword = "android"
            keyAlias = "eve"
            keyPassword = "android"
        }

        // Real release keystore KABHI repo me commit nahi hota. GitHub Actions isko
        // GitHub Secrets se decode karke RELEASE_STORE_FILE env var me path deta hai
        // (dekho .github/workflows/release.yml). Local machine par agar yeh env vars
        // set nahi hain, to release build automatically debug keystore se hi sign ho
        // jaayega (taaki `gradle assembleRelease` local par bhi bina secrets ke chal
        // jaaye) — sirf CI se banaya hua release Play Store ke liye asli signed hota hai.
        create("release") {
            val storeFilePath = System.getenv("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            } else {
                storeFile = file("eve-debug.keystore")
                storePassword = "android"
                keyAlias = "eve"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.activity:activity-ktx:1.9.1")

    // MVVM + Coroutines/Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
