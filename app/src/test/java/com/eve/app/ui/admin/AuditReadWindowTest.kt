package com.eve.app.ui.admin

import com.eve.app.data.repository.AuditLogTimeRange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditReadWindowTest {
    private val request = AuditReadWindow("admin-1", "admin@example.test", 12L,
        AuditLogTimeRange.ALL_TIME, null, "CREATE", "exam", 200L)

    private fun isCurrent(
        uid: String? = "admin-1", email: String? = "admin@example.test", epoch: Long = 12L,
        range: AuditLogTimeRange = AuditLogTimeRange.ALL_TIME, admin: String? = null,
        action: String? = "CREATE", query: String? = "exam"
    ) = request.matches(uid, email, epoch, range, admin, action, query)

    @Test fun matchingReadCanPublishIncludingPageCursor() {
        assertTrue(isCurrent())
        assertTrue(request.before == 200L)
    }

    @Test fun staleFilterOrSearchResultCannotPublish() {
        assertFalse(isCurrent(range = AuditLogTimeRange.TODAY))
        assertFalse(isCurrent(admin = "another-admin@example.test"))
        assertFalse(isCurrent(action = "DELETE"))
        assertFalse(isCurrent(query = "other"))
        assertFalse(isCurrent(epoch = 13L))
    }

    @Test fun revokedOrSwitchedAccountCannotPublish() {
        assertFalse(isCurrent(uid = "different"))
        assertFalse(isCurrent(uid = null))
        assertFalse(isCurrent(email = "different@example.test"))
        assertFalse(isCurrent(email = null))
    }

    @Test fun emptyOriginalIdentitiesAreNeverAuthorized() {
        assertFalse(request.copy(ownerUid = "").matches("", "admin@example.test",
            12L, AuditLogTimeRange.ALL_TIME, null, "CREATE", "exam"))
        assertFalse(request.copy(ownerEmail = "").matches("admin-1", "",
            12L, AuditLogTimeRange.ALL_TIME, null, "CREATE", "exam"))
    }
}
