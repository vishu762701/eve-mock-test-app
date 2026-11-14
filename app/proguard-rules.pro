# keep rules

# Line numbers and stack traces
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Gson & Data Models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.eve.app.data.model.** { *; }
-keep class com.eve.app.data.remote.** { *; }
-keep class com.eve.app.data.local.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Firebase & Play Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# 3D OpenGL Custom Views
-keep class com.eve.app.ui.premium.gl.** { *; }

# Lottie Animations
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# JNI Methods & Telegram Native Components
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.telegram.ui.Components.RLottieNative { *; }
-keep class com.eve.app.telegram.qr.QrBackdropNative { *; }

# Profile Gooey RenderEffect / AGSL / RuntimeShader & View Hierarchy
-keep class com.eve.app.ui.profile.ProfileGooeyView { *; }
-keep class com.eve.app.ui.profile.ProfileGooeyMotionMath { *; }
-keep class com.eve.app.ui.profile.ProfileAvatarTrajectory { *; }
-keep class android.graphics.RenderEffect { *; }
-keep class android.graphics.RuntimeShader { *; }

# Telegram Ports & Custom Design System Components (Reflection / Layout Inflation)
-keep class org.telegram.** { *; }
-keep class com.eve.app.designsystem.** { *; }
-keep class com.eve.app.ui.components.** { *; }

# Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Firebase & Database Models
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
