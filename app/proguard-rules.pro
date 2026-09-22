# keep rules

# Phase 15: Crashlytics ko line numbers ke saath readable stack traces chahiye (agar future me
# isMinifyEnabled = true kiya jaye). Minify abhi off hai isliye yeh rules abhi inactive hain.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
