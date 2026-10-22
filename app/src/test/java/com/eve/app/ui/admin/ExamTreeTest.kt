package com.eve.app.ui.admin

import com.eve.app.data.model.Exam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamTreeTest {
    private fun item(id: String, parent: String = "", name: String = id) =
        ExamWithStats(Exam(id = id, parentExamId = parent, examName = name))

    @Test fun childrenOnlyAppearBelowExpandedParents() {
        val source = listOf(item("main"), item("sub", "main"), item("second", "main"))
        assertEquals(listOf("main"), ExamTree.flatten(source, emptySet(), "").map { it.exam.id })
        val expanded = ExamTree.flatten(source, setOf("main"), "")
        assertEquals(listOf("main", "sub", "second"), expanded.map { it.exam.id })
        assertTrue(expanded[0].hasChildren)
        assertTrue(expanded[0].isExpanded)
        assertEquals(1, expanded[1].treeDepth)
    }

    @Test fun searchRetainsParentsAndOnlyMatchingChildren() {
        val source = listOf(item("p", name = "General"),
            item("c", "p", "Rajasthan history"), item("other", "p", "Physics"))
        val found = ExamTree.flatten(source, emptySet(), "history")
        assertEquals(listOf("p", "c"), found.map { it.exam.id })
        assertTrue(found.first().isExpanded)
        assertEquals(1, found.last().treeDepth)
    }

    @Test fun orphanCycleAndDuplicateIdsRemainFinite() {
        val source = listOf(item("orphan", "missing"), item("a", "b"),
            item("b", "a"), item("dup"), item("dup"))
        val result = ExamTree.flatten(source, setOf("a", "b"), "")
        assertEquals(listOf("orphan", "a", "b", "dup"), result.map { it.exam.id })
        assertTrue(result.all { it.treeDepth == 0 })
        assertFalse(result.any { it.hasChildren })
    }

    @Test fun matchingMainExamRetainsItsChildrenAndDeepParents() {
        val source = listOf(item("p", name = "SSC"), item("c", "p", "Tier 1"),
            item("g", "c", "Phase 2"))
        assertEquals(listOf("p", "c", "g"),
            ExamTree.flatten(source, emptySet(), "SSC").map { it.exam.id })
        assertEquals(listOf("p", "c", "g"),
            ExamTree.flatten(source, emptySet(), "Phase 2").map { it.exam.id })
    }
}
