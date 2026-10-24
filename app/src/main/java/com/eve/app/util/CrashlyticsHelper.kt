package com.eve.app.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Phase 15: crash reports me sirf stack trace dikhta hai, kis user ke saath hua yeh pata nahi
 * chalta — isliye login/logout par Crashlytics ko user ka UID (email nahi, privacy ke liye)
 * aur admin status bata dete hain. Firebase Console -> Crashlytics me ab har crash report ke
 * saath yeh context bhi dikhega.
 */
object CrashlyticsHelper {

    fun identify(uid: String, isAdmin: Boolean) {
        FirebaseCrashlytics.getInstance().apply {
            setUserId(uid)
            setCustomKey("is_admin", isAdmin)
        }
    }

    fun clearIdentity() {
        FirebaseCrashlytics.getInstance().setUserId("")
    }

    /** Non-fatal error ko bhi Crashlytics me log karna ho (app crash na ho par kuch fail ho jaye). */
    fun logNonFatal(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}
