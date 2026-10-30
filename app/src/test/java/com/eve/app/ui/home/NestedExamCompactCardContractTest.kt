package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NestedExamCompactCardContractTest {
    @Test fun theOuterCardAndBareBookIconAreRetained() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/layout/item_sub_exam.xml"))
        val root = document.documentElement
        assertEquals("com.eve.app.designsystem.DesignCardView", root.nodeName)
        assertEquals("wrap_content", root.getAttribute("android:layout_height"))
        assertEquals("56dp", root.getAttribute("android:minHeight"))
        assertEquals("true", root.getAttribute("android:clickable"))
        val elements = document.getElementsByTagName("*")
        val views = (0 until elements.length).map { elements.item(it) as Element }
        val icon = views.first { it.getAttribute("android:id") == "@+id/ivSubExamBook" }
        assertEquals("@drawable/telegram_popup_notes", icon.getAttribute("android:src"))
        assertEquals("@null", icon.getAttribute("android:background"))
        assertEquals("24dp", icon.getAttribute("android:layout_width"))
        val title = views.first { it.getAttribute("android:id") == "@+id/tvSubExamName" }
        assertEquals("0dp", title.getAttribute("android:layout_width"))
        assertEquals("1", title.getAttribute("android:layout_weight"))
    }

    @Test fun subExamsUseTheirOwnCompactLayoutWithoutChangingTestRows() {
        val source = File("src/main/java/com/eve/app/ui/home/ExamTestsAdapter.kt").readText()
        assertTrue(source.contains("VIEW_TYPE_SUB_EXAM -> {"))
        assertTrue(source.contains("ItemSubExamBinding.inflate"))
        assertTrue(source.contains("is ExamTestsListItem.SubExamItem -> (holder as SubExamViewHolder).bind(item)"))
        assertTrue(source.contains("onSubExamClick(item.exam)"))
        assertTrue(source.contains("ItemExamTestBinding.inflate"))
    }
}
