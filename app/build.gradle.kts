import java.io.File

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
    ndkVersion = "27.2.12479018"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = "com.eve.app"
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = appVersionCode
        versionName = appVersionName
        ndk {
            abiFilters += (project.findProperty("eve.nativeAbis")?.toString()?.split(",")
                ?: listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }
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
        // Native radio lifecycle/palette tests need actual EVE theme resources on Robolectric.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Local-only live QR scanning; no Telegram account or network SDK.
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    // Telegram bulletin bottom-offset spring (stiffness 900, no bounce).
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // MVVM + Coroutines/Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
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

    implementation("com.google.zxing:core:3.5.3")

    // Lottie animation library
    implementation("com.airbnb.android:lottie:6.4.1")

    // Real-time frosted-glass BlurView for dialog cards
    implementation("com.github.Dimezis:BlurView:version-2.0.6")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // Android Color/HSV unit tests require a real framework implementation on the JVM.
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

// Print the actual assertion message (including asset path and expected/actual
// SHA-256) for failing JVM tests. This does not change any test result or gate.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}

// The same C++ backdrop raster runs in JVM pixel tests. Only its AndroidBitmap
// API boundary is supplied by a test-only bridge; no production fallback/mock.
val qrBackdropHostLibrary = layout.buildDirectory.file("host-native/${System.mapLibraryName("eve_qr_backdrop_host")}")
val compileQrBackdropHost by tasks.registering(Exec::class) {
    val jdk = File(System.getProperty("java.home"))
    val os = System.getProperty("os.name").lowercase()
    val platform = if (os.contains("mac")) "darwin" else if (os.contains("win")) "win32" else "linux"
    inputs.property("hostJavaVersion", System.getProperty("java.version"))
    inputs.property("hostPlatform", platform)
    inputs.files("src/main/cpp/telegram_qr_backdrop.cpp", "src/test/cpp/qr_bitmap_host.cpp", "src/test/cpp/include/android/bitmap.h")
    outputs.file(qrBackdropHostLibrary)
    doFirst { qrBackdropHostLibrary.get().asFile.parentFile.mkdirs() }
    commandLine("c++", "-std=c++17", if (platform == "darwin") "-dynamiclib" else "-shared", "-fPIC", "-O2",
        "-I${jdk}/include", "-I${jdk}/include/$platform", "-Isrc/test/cpp/include",
        "src/main/cpp/telegram_qr_backdrop.cpp", "src/test/cpp/qr_bitmap_host.cpp", "-o", qrBackdropHostLibrary.get().asFile.absolutePath)
}
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    dependsOn(compileQrBackdropHost)
    inputs.file(qrBackdropHostLibrary).withPropertyName("qrBackdropHostLibrary")
    systemProperty("eve.qr.backdrop.hostLibrary", qrBackdropHostLibrary.get().asFile.absolutePath)
}

// Robolectric closes real synthetic-provider descriptors through java.io reflection on JDK21.
// This applies only to host test JVMs, never the shipped Android application.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
    // Robolectric downloads instrumented SDKs at test runtime, outside Gradle's resolver.
    // Prefer the canonical Maven Central host rather than repeated repo1 rate limits.
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
}
