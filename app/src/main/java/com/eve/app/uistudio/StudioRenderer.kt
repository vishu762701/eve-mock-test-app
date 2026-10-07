package com.eve.app.uistudio

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eve.app.data.model.uistudio.*
import com.eve.app.util.UiStudioEngine
import java.util.WeakHashMap

/** The same binding, insertion and material path is used by production and Studio. */
object StudioRenderer {
    data class Element(val id: String, val view: View, val parent: String?, val repeated: Boolean) {
        val label get() = id.removePrefix("native_").replace('_',' ') + if (repeated) " · all repeated items" else ""
        val dynamic get() = StudioPolicy.isDynamic(id)
        val protected get() = !dynamic // Native structure/actions are owned by the app.
    }
    private val nativeLabels = WeakHashMap<TextView,CharSequence>()
    private val entered = WeakHashMap<View,Boolean>()
    private val applied = WeakHashMap<View, ComponentConfig>()
    private val rendered = WeakHashMap<View,List<Any?>>()
    private fun fingerprint(v: View): List<Any?> = listOf(v.background,(v as? TextView)?.textColors,(v as? TextView)?.textSize,(v as? TextView)?.typeface,v.elevation,v.paddingLeft,v.paddingTop,v.paddingRight,v.paddingBottom,v.layoutParams?.width,v.layoutParams?.height)
    private val aliases = mapOf(
        "hero_banner" to "panelHomeBanner", "streak_pill" to "layoutStreakPill", "search_bar" to "btnSearch", "find_test_panel" to "btnSearch",
        "timer_pill" to "timerCapsuleHost", "question_card" to "questionContainer", "question_text" to "tvQuestion",
        "bottom_actions" to "layoutBottomBar", "action_grid" to "layoutBottomBar", "score_card" to "cardResultHero",
        "analytics_summary" to "rowOverviewStatisticsTiles", "login_hero" to "cardLogin", "login_card" to "cardLogin", "action_buttons" to "layoutBottomBar", "btn_reattempt" to "btnReattempt", "btn_share" to "btnShare"
    )
    fun elements(root: View): List<Element> {
        val out = mutableListOf<Element>()
        fun walk(v: View, parent: String?, repeated: Boolean) {
            if (v is eightbitlab.com.blurview.BlurView || v.tag == "studio_backdrop" || v.tag == "studio_insertions") return
            val key = (v.tag as? String)?.takeIf { StudioPolicy.isDynamic(it) } ?: if (v.id != View.NO_ID) {
                runCatching { "native_" + v.resources.getResourceEntryName(v.id) }.getOrNull()
            } else null
            if (key != null) out += Element(key, v, parent, repeated)
            if (v is ViewGroup) v.children.toList().forEach { walk(it,key ?: parent,repeated || v is RecyclerView) }
        }
        walk(root,null,false)
        // Inserted views live in a reserved host but remain individually selectable.
        fun dynamic(v: View, parent: String?) {
            val key = (v.tag as? String)?.takeIf { StudioPolicy.isDynamic(it) }
            if (key != null) out += Element(key,v,parent,false)
            if (v is ViewGroup) v.children.toList().forEach { dynamic(it,key ?: parent) }
        }
        (root as? ViewGroup)?.findViewWithTag<View>("studio_insertions")?.let { dynamic(it,null) }
        return out
    }
    fun keysFor(id: String): Set<String> = setOf(id) + aliases.filterValues { "native_"+it==id }.keys
    fun configuration(e: Element, components: Map<String,ComponentConfig>): ComponentConfig? {
        val inherited = when(e.id) {
            "native_tvStreakSummary" -> "streak_pill"
            "native_circularTimerView" -> "timer_pill"
            "native_btnClear","native_btnMarkReview","native_btnPrev","native_btnNext" -> "action_buttons"
            "native_rbA","native_rbB","native_rbC","native_rbD" -> "option_item"
            else -> null
        }
        return (keysFor(e.id)+listOfNotNull(inherited)).firstNotNullOfOrNull { components[it] }?.copy(id=e.id)
    }
    fun defaults(e: Element) = ComponentConfig(id=e.id, name=e.label, type=when(e.view) {
        is Button -> "button"; is TextView -> "text"; is ImageView -> "image"; else -> "card"
    }, isProtected=e.protected)

    fun apply(root: View, screenKey: String, config: UiStudioConfig, sandbox: ((ActionProperties)->Unit)? = null, force: Boolean = false): List<Element> {
        val screen = config.screens[screenKey]
        if(!force && entered.put(root,true)==null && screen?.animation?.enabled==true) root.post { UiStudioEngine.playEntranceAnimation(root,screen.animation) }
        val backgroundTarget = root.findViewById<View>(com.eve.app.R.id.mainContentContainer) ?: root
        val background = screen?.backgroundColor ?: config.designSystem.appBackground.takeIf { config.designSystem.enabled }
        if (backgroundTarget.id == View.NO_ID) {
            val bgConfig = ComponentConfig(id="screen", appearance=AppearanceProperties(backgroundColor=background))
            if ((background != null || applied[backgroundTarget]?.id == "screen") && applied[backgroundTarget] != bgConfig) {
                UiStudioEngine.applyToView(backgroundTarget,bgConfig);applied[backgroundTarget]=bgConfig
            }
        }
        val components = screen?.components.orEmpty()
        var list = elements(root)
        for (e in list.filter { !it.dynamic }) {
            val c = configuration(e,components)
            val screenColor = background.takeIf { e.view === backgroundTarget }
            val globalText = config.branding.brandColor.takeIf { config.branding.enabled && e.id in setOf("native_tvLogo","native_tvAppName") }
                ?: config.designSystem.textPrimary.takeIf { config.designSystem.enabled && e.view is TextView }
            val source = c ?: defaults(e)
            val safe = if (c != null || screenColor != null || globalText != null) source.copy(
                id=e.id, isProtected=e.protected, visible=source.visible, enabled=true, content=ContentProperties(), actions=ActionProperties(),
                appearance=source.appearance.copy(backgroundColor=source.appearance.backgroundColor ?: screenColor,opacity=source.appearance.opacity?.coerceIn(.3f,1f)),
                typography=source.typography.copy(textColor=source.typography.textColor ?: globalText), animation=source.animation.copy(enabled=false)
            ) else null
            val previous = applied[e.view]
            if (safe != null && (safe != previous || force || rendered[e.view] != fingerprint(e.view))) {
                UiStudioEngine.applyToView(e.view,safe)
                applied[e.view]=safe
                if(e.view is com.eve.app.ui.common.CircularTimerView) e.view.setTimerColors(safe.appearance.strokeColor ?: safe.typography.textColor,safe.states.disabledBackgroundColor,safe.appearance.strokeColor,safe.typography.textColor)
                rendered[e.view]=fingerprint(e.view)
                if (c?.animation?.enabled == true && previous == null) UiStudioEngine.playEntranceAnimation(e.view,c.animation)
            } else if (safe == null && previous != null && previous.id != "screen") {
                UiStudioEngine.applyToView(e.view,defaults(e))
                if(e.view is com.eve.app.ui.common.CircularTimerView)e.view.setTimerColors(null,null,null,null)
                applied.remove(e.view);rendered.remove(e.view)
            }
            if (e.view is TextView && e.id in setOf("native_tvLogo","native_tvAppName")) {
                val text = nativeLabels.getOrPut(e.view) { e.view.text }
                if(config.branding.enabled) {
                    e.view.text = if(e.id=="native_tvLogo")config.branding.shortName else config.branding.appDisplayName
                    UiStudioEngine.parseColorSafe(config.branding.brandColor)?.let { e.view.setTextColor(it) }
                } else if(c?.content?.title != null) e.view.text = c.content.title else e.view.text=text
            }
        }
        if (root is ViewGroup && !force) syncInsertions(root,components,sandbox)
        list=elements(root)
        return list
    }
    private class InsertionViewport(context: android.content.Context): ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int,heightMeasureSpec: Int) {
            val available=(rootView.height.takeIf{it>0} ?: resources.displayMetrics.heightPixels)/3
            val incoming=View.MeasureSpec.getSize(heightMeasureSpec)
            val cap=if(View.MeasureSpec.getMode(heightMeasureSpec)==View.MeasureSpec.UNSPECIFIED)available else minOf(available,incoming)
            super.onMeasure(widthMeasureSpec,View.MeasureSpec.makeMeasureSpec(cap.coerceAtLeast(1),View.MeasureSpec.AT_MOST))
        }
    }
    private fun insertionParent(root: ViewGroup): ViewGroup? {
        // Use a real vertical content container. Never add siblings into adapter-owned or constraint layouts.
        fun find(v: View): LinearLayout? {
            if (v is RecyclerView) return null
            if (v is LinearLayout && v.orientation == LinearLayout.VERTICAL && v.childCount > 0) return v
            if (v is ViewGroup) for (child in v.children) find(child)?.let { return it }
            return null
        }
        return find(root)
    }
    fun canInsert(root: View) = root is ViewGroup && insertionParent(root) != null
    private fun syncInsertions(root: ViewGroup, components: Map<String,ComponentConfig>, sandbox: ((ActionProperties)->Unit)?) {
        val custom = components.values.filter { StudioPolicy.isDynamic(it.id) && it.type in StudioPolicy.dynamicTypes }.sortedWith(compareBy({it.order},{it.id}))
        var host = root.findViewWithTag<LinearLayout>("studio_insertions")
        if (custom.isEmpty()) {
            host?.let { val viewport=it.parent as? InsertionViewport;val remove=viewport ?: it;(remove.parent as? ViewGroup)?.removeView(remove) }
            return
        }
        if (host == null) {
            val parent = insertionParent(root) ?: return
            host=LinearLayout(root.context).apply { orientation=LinearLayout.VERTICAL; tag="studio_insertions" }
            val before = parent.children.indexOfFirst { it.layoutParams is LinearLayout.LayoutParams && (it.layoutParams as LinearLayout.LayoutParams).weight > 0f }
            val viewport=InsertionViewport(root.context).apply { tag="studio_insertion_viewport";addView(host,FrameLayout.LayoutParams(-1,-2));isFillViewport=false }
            parent.addView(viewport,if(before >= 0)before else 0,LinearLayout.LayoutParams(-1,-2))
        }
        val existing = elements(root).filter { it.dynamic }.associate { it.id to it.view }
        existing.filterKeys { key -> custom.none { it.id==key } }.values.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        val parents = mutableMapOf<String,ViewGroup>()
        val pending=custom.toMutableList()
        repeat(custom.size) {
            val ready=pending.filter { it.parentId==null || parents.containsKey(it.parentId) }
            ready.forEach { c ->
                val parent=parents[c.parentId] ?: host!!
                val view=existing[c.id] ?: create(root,c)
                if (view.parent !== parent) { (view.parent as? ViewGroup)?.removeView(view); parent.addView(view,ViewGroup.LayoutParams(-1,-2)) }
                val previous=applied[view]
                if (previous != c) {
                    UiStudioEngine.applyToView(view,c.copy(animation=c.animation.copy(enabled=false)))
                    if (view is TextView) view.text=c.content.title.orEmpty()
                    if (view is ImageView && c.content.imageSource != previous?.content?.imageSource) {
                        if (StudioPolicy.safeUrl(c.content.imageSource)) view.load(c.content.imageSource) else view.setImageDrawable(null)
                    }
                    if (view is ImageView && c.type=="icon") {
                        val icons=mapOf("star" to android.R.drawable.btn_star_big_on,"info" to android.R.drawable.ic_dialog_info,"mail" to android.R.drawable.ic_dialog_email)
                        view.setImageResource(icons[c.content.icon] ?: android.R.drawable.ic_dialog_info)
                    }
                    view.contentDescription=c.name
                    if(c.actions.actionType=="none")view.setOnClickListener(null)
                    else view.setOnClickListener { if (sandbox != null) sandbox(c.actions) else execute(root,c.actions) }
                    view.isClickable=c.actions.actionType!="none" || view is Button
                    applied[view]=c
                    if (c?.animation?.enabled == true) UiStudioEngine.playEntranceAnimation(view,c.animation)
                }
                if (view is FrameLayout) {
                    var children=view.findViewWithTag<LinearLayout>("studio_children")
                    if (children==null) { children=LinearLayout(root.context).apply { orientation=LinearLayout.VERTICAL; tag="studio_children" }; view.addView(children,FrameLayout.LayoutParams(-1,-2)) }
                    parents[c.id]=children
                }
                pending.remove(c)
            }
        }
        // Only reorder inserted siblings; native layout constraints remain untouched.
        (listOfNotNull(host)+parents.values).forEach { parent ->
            val ordered=parent.children.filter { StudioPolicy.isDynamic(it.tag as? String ?: "") }.sortedBy { components[it.tag]?.order ?: 0 }.toList()
            ordered.forEachIndexed { index,v -> if (parent.indexOfChild(v)!=index) { parent.removeView(v); parent.addView(v,index) } }
        }
    }
    private fun create(root: View, c: ComponentConfig): View = (when(c.type) {
        "text" -> TextView(root.context)
        "button" -> Button(root.context)
        "image", "icon" -> ImageView(root.context).apply { adjustViewBounds=true; minimumHeight=UiStudioEngine.dpToPx(context,48) }
        "card", "banner" -> FrameLayout(root.context)
        else -> View(root.context)
    }).apply { tag=c.id }

    fun execute(root: View, a: ActionProperties) {
        if (!StudioPolicy.safeAction(a)) return
        val activity=root.context as? Activity ?: return
        if (a.actionType=="open_url") runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(a.actionTarget)).addCategory(Intent.CATEGORY_BROWSABLE)) }
        if (a.actionType=="navigate") {
            val packages=mapOf("home" to "home.MainActivity","profile" to "profile.ProfileActivity","notifications" to "notifications.NotificationsActivity","syllabus" to "syllabus.SyllabusActivity","settings" to "settings.SettingsActivity","history" to "history.HistoryActivity","bookmarks" to "bookmarks.BookmarksActivity","about" to "about.AboutActivity")
            val name=packages[a.actionTarget] ?: return
            runCatching { activity.startActivity(Intent().setClassName(activity,"com.eve.app.ui.$name")) }
        }
    }
}
