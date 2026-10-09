package com.eve.app.data.local

object TestSessionPolicy {
    fun canRestore(session: TestSession?, owner: String, examKey: String, attemptId: String): Boolean =
        session != null && owner.isNotBlank() && attemptId.isNotBlank() &&
            session.userId == owner && session.attemptKey == examKey &&
            session.clientAttemptId == attemptId
}
