plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    // Phase 15: Crashlytics ke liye zaroori Gradle plugin (crash reports register karta hai)
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}
