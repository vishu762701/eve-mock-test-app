package com.eve.app.ui.admin

/** Typed, server-authoritative publication acknowledgment for Generate Now. */
internal data class GeneratedNowAck(
    val testId: String,
    val jobId: String,
    val questionCount: Int
) {
    companion object {
        fun from(payload: Map<String, Any>?, requestedCount: Int): GeneratedNowAck? {
            val test = (payload?.get("testId") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val job = (payload["jobId"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            // Retrofit/Gson decodes untyped JSON numbers as Double.
            val number = (payload["questionCount"] as? Number)?.toDouble() ?: return null
            if (!number.isFinite() || number % 1.0 != 0.0 ||
                number !in 1.0..200.0 || number != requestedCount.toDouble()) return null
            return GeneratedNowAck(test, job, number.toInt())
        }
    }
}
