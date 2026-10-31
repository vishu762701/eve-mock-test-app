package com.eve.app.data.remote

import java.io.IOException

/** OkHttp-only metadata, never a header/body/QR field. Binds queued profile work to its initiating account. */
data class ProfileRequestOwner(val uid: String) {
    init { require(uid.isNotBlank()) }
    fun requireMatches(actualUid: String?) {
        if (actualUid != uid) throw IOException("Account changed. Reopen your profile before retrying.")
    }
}
