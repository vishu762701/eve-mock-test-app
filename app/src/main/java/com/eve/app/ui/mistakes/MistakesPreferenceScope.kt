package com.eve.app.ui.mistakes

import java.security.MessageDigest

/** Owner-scoped local learned flags. Legacy global eve_mistakes has no proven owner. */
internal object MistakesPreferenceScope {
    private const val PREFIX = "eve_mistakes_u_v2_"

    fun namespaceForUser(uid: String): String {
        require(uid.isNotBlank()) { "Cannot read or write unowned learned flags" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return PREFIX + digest
    }
}
