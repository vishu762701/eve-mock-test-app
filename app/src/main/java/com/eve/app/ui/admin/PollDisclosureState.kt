package com.eve.app.ui.admin

/**
 * Independent expanded state per persisted poll ID. A refresh keeps surviving IDs,
 * while removed polls cannot leak their state to subsequently recycled rows.
 */
internal class PollDisclosureState {
    private val expandedIds = mutableSetOf<String>()

    fun isExpanded(id: String): Boolean = id in expandedIds

    fun toggle(id: String): Boolean {
        if (id in expandedIds) {
            expandedIds.remove(id)
            return false
        }
        expandedIds.add(id)
        return true
    }

    fun retain(ids: Collection<String>) {
        expandedIds.retainAll(ids.toSet())
    }
}
