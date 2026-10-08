package com.eve.app.util

/** Retain answers on every failure; replay only recoverable responses for the original owner. */
object SubmissionRetryPolicy {
    fun shouldRetryHttp(status: Int): Boolean = status == 401 || status == 408 || status == 429 || status in 500..599
    fun ownsSubmission(ownerUid: String?, currentUid: String?): Boolean = !ownerUid.isNullOrBlank() && ownerUid == currentUid
}
