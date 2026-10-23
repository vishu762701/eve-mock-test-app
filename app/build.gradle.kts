plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    // Phase 15: Crashlytics + Analytics
    id("com.google.firebase.crashlytics")
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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eve.app"
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

        // A production release must never be signed by the committed debug key,
        // even in local/Termux builds. Release tasks fail without all credentials.
        create("release") {
            val storeFilePath = System.getenv("RELEASE_STORE_FILE")
            val storePasswordEnv = System.getenv("RELEASE_STORE_PASSWORD")
            val keyAliasEnv = System.getenv("RELEASE_KEY_ALIAS")
            val keyPasswordEnv = System.getenv("RELEASE_KEY_PASSWORD")
            val isReleaseTaskRequested = gradle.startParameter.taskNames.any {
                it.contains("Release", ignoreCase = true)
            }
            val validKeystore = !storeFilePath.isNullOrBlank() && file(storeFilePath).isFile
            val validCredentials = !storePasswordEnv.isNullOrBlank() &&
                !keyAliasEnv.isNullOrBlank() && !keyPasswordEnv.isNullOrBlank()
            if (isReleaseTaskRequested && (!validKeystore || !validCredentials)) {
                throw org.gradle.api.GradleException(
                    "Refusing release build: a real RELEASE_STORE_FILE and complete signing " +
                        "credentials are required; the debug key is never a release fallback."
                )
            }
            if (validKeystore && validCredentials) {
                storeFile = file(storeFilePath!!)
                storePassword = storePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // MVVM + Coroutines/Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Retrofit & OkHttp Networking for Cloudflare Worker API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Firebase (Auth-only migration + FCM, Crashlytics, Analytics)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Phase 15: Crashlytics (crash reports) + Analytics (kaunsa exam sabse zyada attempt
    // ho raha, kahan drop-off ho raha — dono Firebase Console me dikhte hain)
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // Push notifications (Phase 12): naya-exam alerts (FCM topic) + daily reminder (WorkManager)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Profile screen: Google account ki photo (remote URL) load karne ke liye lightweight image loader
    implementation("io.coil-kt:coil:2.6.0")

    // Lottie animation library
    implementation("com.airbnb.android:lottie:6.4.1")

    // Real-time frosted-glass BlurView for dialog cards
    implementation("com.github.Dimezis:BlurView:version-2.0.6")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
