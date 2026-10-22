package com.eve.app.ui.admin

/** Admin-only tree projection. Never modifies the Exam.parentExamId data contract. */
internal object ExamTree {
    fun flatten(
        original: List<ExamWithStats>,
        expandedIds: Set<String>,
        search: String
    ): List<ExamWithStats> {
        val rows = original.filter { it.exam.id.isNotBlank() }.distinctBy { it.exam.id }
        val byId = rows.associateBy { it.exam.id }
        val children = mutableMapOf<String, MutableList<ExamWithStats>>()
        val roots = mutableListOf<ExamWithStats>()

        fun validParent(row: ExamWithStats): String? {
            val id = row.exam.id
            val parent = row.exam.parentExamId
            if (parent.isBlank() || parent == id || !byId.containsKey(parent)) return null
            // Follow the real parent chain; a malformed cycle becomes a root.
            val visited = mutableSetOf(id)
            var cursor = parent
            while (cursor.isNotBlank()) {
                if (!visited.add(cursor)) return null
                cursor = byId[cursor]?.exam?.parentExamId.orEmpty()
            }
            return parent
        }
        for (row in rows) {
            val parent = validParent(row)
            if (parent == null) roots.add(row)
            else children.getOrPut(parent) { mutableListOf() }.add(row)
        }

        val trimmed = search.trim()
        val matched = mutableSetOf<String>()
        if (trimmed.isNotEmpty()) {
            for (row in rows) {
                if (row.exam.examName.contains(trimmed, ignoreCase = true) ||
                    row.exam.categoryOrOther.contains(trimmed, ignoreCase = true)
                ) {
                    var cursor = row.exam.id
                    val seen = mutableSetOf<String>()
                    while (cursor.isNotBlank() && seen.add(cursor)) {
                        matched.add(cursor)
                        cursor = validParent(byId[cursor] ?: break).orEmpty()
                    }
                    // When a parent itself matches, keep its sub-exams in context.
                    fun includeDescendants(parentId: String) {
                        for (child in children[parentId].orEmpty()) {
                            if (matched.add(child.exam.id)) includeDescendants(child.exam.id)
                        }
                    }
                    includeDescendants(row.exam.id)
                }
            }
        }
        val result = mutableListOf<ExamWithStats>()
        fun visit(row: ExamWithStats, depth: Int) {
            val id = row.exam.id
            if (trimmed.isNotEmpty() && id !in matched) return
            val hasChildren = children[id].orEmpty().isNotEmpty()
            val open = hasChildren && (trimmed.isNotEmpty() || id in expandedIds)
            result.add(row.copy(treeDepth = depth, hasChildren = hasChildren, isExpanded = open))
            if (open) children[id].orEmpty().forEach { visit(it, depth + 1) }
        }
        roots.forEach { visit(it, 0) }
        return result
    }
}
