package com.eve.app.data.model

data class AdminAuditLog(
    val id: String = "",
    val actionType: String = "",
    val description: String = "",
    val adminEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val EXAM_CREATED = "EXAM_CREATED"
        const val EXAM_EDITED = "EXAM_EDITED"
        const val EXAM_DELETED = "EXAM_DELETED"
        const val BROADCAST_SENT = "BROADCAST_SENT"
        const val BROADCAST_DELETED = "BROADCAST_DELETED"
        const val FEEDBACK_POST_CREATED = "FEEDBACK_POST_CREATED"
        const val FEEDBACK_POST_EDITED = "FEEDBACK_POST_EDITED"
        const val FEEDBACK_POST_DELETED = "FEEDBACK_POST_DELETED"
        const val USER_STATUS_TOGGLED = "USER_STATUS_TOGGLED"
        const val ADMIN_ADDED = "ADMIN_ADDED"
        const val ADMIN_REMOVED = "ADMIN_REMOVED"
        const val MAINTENANCE_TOGGLED = "MAINTENANCE_TOGGLED"
        const val GENERATE_NOW_TRIGGERED = "GENERATE_NOW_TRIGGERED"

        // Aliases with ACTION_ prefix for seamless compatibility
        const val ACTION_EXAM_CREATED = EXAM_CREATED
        const val ACTION_EXAM_EDITED = EXAM_EDITED
        const val ACTION_EXAM_DELETED = EXAM_DELETED
        const val ACTION_BROADCAST_SENT = BROADCAST_SENT
        const val ACTION_BROADCAST_DELETED = BROADCAST_DELETED
        const val ACTION_FEEDBACK_POST_CREATED = FEEDBACK_POST_CREATED
        const val ACTION_FEEDBACK_POST_EDITED = FEEDBACK_POST_EDITED
        const val ACTION_FEEDBACK_POST_DELETED = FEEDBACK_POST_DELETED
        const val ACTION_USER_STATUS_TOGGLED = USER_STATUS_TOGGLED
        const val ACTION_ADMIN_ADDED = ADMIN_ADDED
        const val ACTION_ADMIN_REMOVED = ADMIN_REMOVED
        const val ACTION_MAINTENANCE_TOGGLED = MAINTENANCE_TOGGLED
        const val ACTION_GENERATE_NOW_TRIGGERED = GENERATE_NOW_TRIGGERED
    }

    val actionBadge: String get() = when (actionType) {
        EXAM_CREATED -> "✨ New Exam"
        EXAM_EDITED -> "✏️ Edit Exam"
        EXAM_DELETED -> "🗑️ Delete Exam"
        BROADCAST_SENT -> "📢 Broadcast Sent"
        BROADCAST_DELETED -> "🗑️ Broadcast Deleted"
        FEEDBACK_POST_CREATED -> "💬 New Feedback Post"
        FEEDBACK_POST_EDITED -> "✏️ Edit Feedback Post"
        FEEDBACK_POST_DELETED -> "🗑️ Delete Feedback Post"
        USER_STATUS_TOGGLED -> "👤 User Status"
        ADMIN_ADDED -> "🛡️ Admin Added"
        ADMIN_REMOVED -> "🛡️ Admin Removed"
        MAINTENANCE_TOGGLED -> "🔧 Maintenance Mode"
        GENERATE_NOW_TRIGGERED -> "⚡ Generate Now"
        else -> "📋 $actionType"
    }

    data class BadgeInfo(val text: String, val colorRes: Int = 0)

    fun getActionBadge(): BadgeInfo = BadgeInfo(actionBadge)
}
