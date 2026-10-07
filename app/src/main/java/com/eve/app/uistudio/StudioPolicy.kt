package com.eve.app.uistudio

import com.eve.app.data.model.uistudio.*
import java.net.URI

/** Shared editor/runtime policy. Native business bindings always own their data and actions. */
object StudioPolicy {
    val dynamicTypes = listOf("text", "button", "image", "icon", "card", "banner", "divider", "spacer")
    val destinations = setOf("home", "profile", "notifications", "syllabus", "settings", "history", "bookmarks", "about")
    fun isDynamic(id: String) = id.startsWith("custom_")
    fun safeUrl(value: String?): Boolean = try {
        val u = URI(value ?: "")
        u.scheme in listOf("https", "http") && !u.host.isNullOrBlank() && u.rawUserInfo == null && !value.orEmpty().contains('\\')
    } catch (_: Exception) { false }
    fun safeAction(a: ActionProperties): Boolean = when (a.actionType) {
        "none" -> true
        "navigate" -> a.actionTarget in destinations
        "open_url" -> safeUrl(a.actionTarget)
        else -> false
    }
    fun validate(config: UiStudioConfig): List<String> {
        val errors = mutableListOf<String>()
        config.screens.forEach { (screenKey, screen) ->
            screen.components.forEach { (key, c) ->
                val path = "$screenKey.$key"
                if (c.id.isNotBlank() && c.id != key) errors += "$path: ID must match key"
                if (!safeAction(c.actions)) errors += "$path: unsupported action or destination"
                if (!isDynamic(key) && (c.actions.actionType != "none" || !c.enabled)) errors += "$path: native actions are protected"
                if ((c.isProtected || key.startsWith("native_")) && !c.visible) errors += "$path: essential control cannot be hidden"
                if (isDynamic(key) && c.type !in dynamicTypes) errors += "$path: unsupported inserted type"
                if (c.parentId != null && isDynamic(key)) {
                    val parent = screen.components[c.parentId]
                    if (parent == null || !isDynamic(parent.id) || parent.type !in listOf("card", "banner")) errors += "$path: choose an inserted card/banner parent"
                }
                val visited = mutableSetOf(key)
                var p = c.parentId
                while (p != null) {
                    if (!visited.add(p)) { errors += "$path: hierarchy cycle"; break }
                    p = screen.components[p]?.parentId
                }
                c.content.imageSource?.let { if (it.isNotBlank() && !safeUrl(it)) errors += "$path: invalid image URL" }
                listOf(c.material.materialOpacity, c.material.tintOpacity, c.appearance.opacity).forEach {
                    if (it != null && (!it.isFinite() || it !in 0f..1f)) errors += "$path: opacity must be 0–1"
                }
                if(!isDynamic(key) && c.appearance.opacity != null && c.appearance.opacity < .3f)errors += "$path: native opacity must remain at least 30%"
                listOf(c.layout.width,c.layout.height).forEach { value ->
                    if(value != null) {
                        val d=value.trim().lowercase()
                        val n=d.removeSuffix("dp").removeSuffix("px").toIntOrNull()
                        if(d !in listOf("match_parent","match","wrap_content","wrap","auto") && (n==null || n !in 0..2000))errors += "$path: dimensions must be 0–2000 dp or match/wrap"
                    }
                }
                c.material.blurRadius?.let { if (it !in 0..50) errors += "$path: blur must be 0–50" }
                c.typography.fontFamily?.let { if(it !in listOf("sans-serif","serif","monospace"))errors += "$path: unsupported font" }
                if (c.animation.type !in listOf("fade", "scale", "fade_scale", "slide", "pop")) errors += "$path: unsupported animation"
            }
        }
        return errors
    }
}
