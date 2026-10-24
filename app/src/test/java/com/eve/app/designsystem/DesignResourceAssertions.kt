package com.eve.app.designsystem

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Resolve shared styles in source-level XML assertions, just as inflation does. */
fun Element.designAttribute(name: String): String {
    if (hasAttribute(name)) return getAttribute(name)
    val resources = listOf(File("src/main/res/values"), File("app/src/main/res/values")).first { it.isDirectory }
    val styles = mutableMapOf<String, Element>()
    resources.listFiles()!!.filter { it.extension == "xml" }.forEach { file ->
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("style")
        for (i in 0 until nodes.length) (nodes.item(i) as Element).let { styles[it.getAttribute("name")] = it }
    }
    fun resolve(style: String, seen: Set<String> = emptySet()): String {
        if (style in seen) error("Cyclic style $style")
        val node = styles[style] ?: return ""
        val items = node.getElementsByTagName("item")
        for (i in 0 until items.length) (items.item(i) as Element).let {
            if (it.getAttribute("name") == name.removePrefix("app:")) return it.textContent.trim()
        }
        val parent = if (node.hasAttribute("parent")) node.getAttribute("parent").removePrefix("@style/") else style.substringBeforeLast('.', "")
        return if (parent.isNotEmpty()) resolve(parent, seen + style) else ""
    }
    return resolve(getAttribute("style").removePrefix("@style/"))
}
