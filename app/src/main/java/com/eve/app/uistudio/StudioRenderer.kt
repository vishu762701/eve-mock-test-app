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
        val protected get() = StudioPolicy.behaviorProtected(id) // Native structure/actions are owned by the app.
    }
    private val nativeLabels = WeakHashMap<TextView,CharSequence>()
    private val entered = WeakHashMap<View,AnimationProperties>()
    private val componentEntrances = WeakHashMap<View,AnimationProperties>()
    private val applied = WeakHashMap<View, ComponentConfig>()
    private fun fingerprint(v: View, config: ComponentConfig?): List<Any?> {
        val card = v as? com.google.android.material.card.MaterialCardView
        val button = v as? com.google.android.material.button.MaterialButton
        val margins = v.layoutParams as? ViewGroup.MarginLayoutParams
        return listOf(v.background, (v as? TextView)?.textColors, (v as? TextView)?.textSize,
            (v as? TextView)?.typeface, v.elevation, v.paddingLeft, v.paddingTop, v.paddingRight,
            v.paddingBottom, v.layoutParams?.width, v.layoutParams?.height,
            margins?.leftMargin, margins?.topMargin, margins?.rightMargin, margins?.bottomMargin, margins?.marginStart, margins?.marginEnd,
            StudioInteraction.stableColors(v), config?.appearance?.opacity?.let { UiStudioEngine.stableAlpha(v) },
            card?.radius, card?.strokeWidth, card?.strokeColorStateList,
            button?.cornerRadius, button?.strokeWidth, button?.strokeColor, button?.iconTint,
            (v as? ImageView)?.imageTintList, (v as? ImageView)?.colorFilter)
    }
    private val aliases = mapOf(
        "native_path_panelHomeBanner_View_0" to "homeBannerMatte",
        "hero_banner" to "panelHomeBanner", "streak_pill" to "layoutStreakPill", "search_bar" to "btnSearch", "find_test_panel" to "btnSearch",
        "timer_pill" to "timerCapsuleHost", "question_card" to "questionContainer", "question_text" to "tvQuestion",
        "bottom_actions" to "layoutBottomBar", "action_grid" to "layoutBottomBar", "score_card" to "cardResultHero",
        "analytics_summary" to "rowOverviewStatisticsTiles", "login_hero" to "cardLogin", "login_card" to "cardLogin", "action_buttons" to "layoutBottomBar", "btn_reattempt" to "btnReattempt", "btn_share" to "btnShare"
    )
    private fun structuralPath(v:View,parent:String?):String {
        val siblings=(v.parent as? ViewGroup)?.children?.filter{it.javaClass==v.javaClass && it.tag!="studio_glass_host"}?.toList().orEmpty()
        val index=if(v.parent is RecyclerView)0 else siblings.indexOf(v).coerceAtLeast(0)
        return "native_path_${parent?.removePrefix("native_") ?: "root"}_${v.javaClass.simpleName}_$index"
    }
    private fun meaningful(v:View)=v is TextView || v is ImageView || v is ViewGroup
    fun elements(root: View): List<Element> {
        val out = mutableListOf<Element>()
        fun walk(v: View, parent: String?, repeated: Boolean, path:String?) {
            if (v is eightbitlab.com.blurview.BlurView || v.tag == "studio_backdrop" || v.tag == "studio_glass_host" || v.tag == "studio_insertions") return
            val key = (v.tag as? String)?.takeIf { StudioPolicy.isDynamic(it) } ?: if (v.id != View.NO_ID) {
                runCatching { "native_" + v.resources.getResourceEntryName(v.id) }.getOrNull() ?: structuralPath(v,path).takeIf{meaningful(v)}
            } else structuralPath(v,path).takeIf{meaningful(v)}
            if (key != null) out += Element(key, v, parent, repeated)
            if (v is ViewGroup) v.children.toList().forEach { walk(it,key ?: parent,repeated || v is RecyclerView,key ?: structuralPath(v,path)) }
        }
        walk(root,null,false,null)
        // Inserted views live in a reserved host but remain individually selectable.
        fun dynamic(v: View, parent: String?) {
            val key = (v.tag as? String)?.takeIf { StudioPolicy.isDynamic(it) }
            if (key != null) out += Element(key,v,parent,false)
            if (v is ViewGroup) v.children.toList().forEach { dynamic(it,key ?: parent) }
        }
        (root as? ViewGroup)?.findViewWithTag<View>("studio_insertions")?.let { dynamic(it,null) }
        val counts=out.groupingBy{it.id}.eachCount()
        return out.map{if((counts[it.id] ?: 0)>1)it.copy(repeated=true)else it}
    }
    fun keysFor(id: String): Set<String> = setOf(id) + aliases.filterValues { "native_"+it==id }.keys
    fun configuration(e: Element, components: Map<String,ComponentConfig>): ComponentConfig? {
        val inherited = when(e.id) {
            "native_tvStreakSummary" -> "streak_pill"
            "native_circularTimerView" -> "timer_pill"
            "native_tvScore", "native_tvScorePercentage" -> "score_card"
            "native_btnClear","native_btnMarkReview","native_btnPrev","native_btnNext" -> "action_buttons"
            "native_rbA","native_rbB","native_rbC","native_rbD" -> "option_item"
            else -> null
        }
        val direct=keysFor(e.id).firstNotNullOfOrNull { components[it] }
        if(direct!=null)return direct.copy(id=e.id)
        val source=inherited?.let { components[it] } ?: return null
        if(inherited=="score_card")return ComponentConfig(id=e.id,typography=source.typography)
        if(e.id=="native_circularTimerView")return ComponentConfig(id=e.id,typography=source.typography,
            appearance=AppearanceProperties(strokeColor=source.appearance.strokeColor),states=source.states)
        return source.copy(id=e.id)
    }
    fun defaults(e: Element) = ComponentConfig(id=e.id, name=e.label, type=when(e.view) {
        is Button -> "button"; is TextView -> "text"; is ImageView -> "image"; else -> "card"
    }, isProtected=e.protected)

    fun apply(root: View, screenKey: String, config: UiStudioConfig, sandbox: ((ActionProperties)->Unit)? = null, force: Boolean = false): List<Element> {
        val screen = config.screens[screenKey]
        if(!force && screen?.animation?.enabled==true && entered[root]!=screen.animation) {
            entered[root]=screen.animation
            root.post { UiStudioEngine.playEntranceAnimation(root,screen.animation) }
        } else if(screen?.animation?.enabled!=true) {
            if(entered.remove(root)!=null)UiStudioEngine.cancelMotion(root)
        }
        val backgroundTarget = root.findViewById<View>(com.eve.app.R.id.mainContentContainer) ?: root
        val background = screen?.backgroundColor ?: config.designSystem.appBackground.takeIf { config.designSystem.enabled }
        // Anonymous layout roots are elements too; one render path owns their background/fingerprint.
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
                appearance=source.appearance.copy(backgroundColor=source.appearance.backgroundColor ?: screenColor,opacity=source.appearance.opacity?.coerceIn(0f,1f)),
                typography=source.typography.copy(textColor=source.typography.textColor ?: globalText), animation=source.animation.copy(enabled=false)
            ) else null
            val previous = applied[e.view]
            if (safe != null && (safe != previous || force || e.view.getTag(com.eve.app.R.id.studio_render_fingerprint) != fingerprint(e.view,safe))) {
                StudioBaseline.refreshNative(e.view)
                UiStudioEngine.applyToView(e.view,safe)
                applied[e.view]=safe
                if(e.view is com.eve.app.ui.common.CircularTimerView) e.view.setTimerColors(safe.appearance.strokeColor,safe.states.disabledBackgroundColor,safe.appearance.strokeColor,safe.typography.textColor).also { e.view.setStudioTypography(safe.typography) }
                e.view.setTag(com.eve.app.R.id.studio_render_fingerprint,fingerprint(e.view,safe))
            } else if (safe == null && previous != null && previous.id != "screen") {
                StudioBaseline.refreshNative(e.view)
                UiStudioEngine.applyToView(e.view,defaults(e))
                if(e.view is com.eve.app.ui.common.CircularTimerView)e.view.setTimerColors(null,null,null,null).also { e.view.setStudioTypography(null) }
                applied.remove(e.view);e.view.setTag(com.eve.app.R.id.studio_render_fingerprint,null)
            }
            if(c?.animation?.enabled==true && componentEntrances[e.view]!=c.animation) {
                componentEntrances[e.view]=c.animation
                UiStudioEngine.playEntranceAnimation(e.view,c.animation)
            } else if(c?.animation?.enabled!=true && componentEntrances.remove(e.view)!=null) {
                UiStudioEngine.cancelMotion(e.view)
            }
            if (e.view is TextView && e.id in setOf("native_tvLogo","native_tvAppName")) {
                val text = nativeLabels.getOrPut(e.view) { e.view.text }
                val desired = if(config.branding.enabled) {
                    if(e.id=="native_tvLogo")config.branding.shortName else config.branding.appDisplayName
                } else c?.content?.title ?: text
                if(e.view.text.toString()!=desired.toString())e.view.text=desired
            }
        }
        if (root is ViewGroup && !force) syncInsertions(root,components,sandbox)
        list=elements(root)
        return list
    }
    /** Publication preflight uses the real layout and adapter views, not the requested type label. */
    fun unsupported(root: View, screenKey: String, config: UiStudioConfig): List<String> {
        val list=elements(root)
        return config.screens[screenKey]?.components.orEmpty().flatMap { (key,c) ->
            val targets=list.filter { key in keysFor(it.id) || configuration(it,mapOf(key to c))!=null }
            if(targets.isEmpty()) return@flatMap listOf("$screenKey.$key: no matching production view")
            targets.flatMap { e ->
                // Legacy surfaces route only their typography to children, not surface blur/fill.
                val effective=configuration(e,mapOf(key to c)) ?: c
                val errors=mutableListOf<String>()
                val prefix="$screenKey.$key: Unsupported property"
                if((effective.material.blurRadius ?: 0)>25) errors += "$prefix blur radius above 25; reduce it to the supported range"
                if((effective.material.blurRadius ?: 0)>0 && !StudioMaterial.supported(e.view))
                    errors += "$prefix backdrop blur; select a containing card/panel or button in a supported parent"
                val legacyTypographyHost=key in setOf("timer_pill","score_card") && key in keysFor(e.id)
                if(effective.typography!=TypographyProperties() && !legacyTypographyHost && e.view !is TextView && e.view !is com.eve.app.ui.common.CircularTimerView)
                    errors += "$prefix typography; select the child text element"
                if(e.view is com.eve.app.ui.common.CircularTimerView && (effective.typography.textAlign!=null || effective.typography.maxLines!=null))
                    errors += "$prefix timer alignment/line count; the countdown stays centered on one line"
                if(effective.states.selectedTextColor!=null && e.view !is TextView)
                    errors += "$prefix selected text color; select the child text element"
                if(effective.appearance.iconTint!=null && e.view !is ImageView && e.view !is com.google.android.material.button.MaterialButton &&
                    (e.view as? TextView)?.compoundDrawables?.any { it!=null }!=true)
                    errors += "$prefix icon tint; select the child icon/image"
                errors
            }
        }.distinct()
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
            val current=parent.children.filter { StudioPolicy.isDynamic(it.tag as? String ?: "") }.toList()
            if(current!=ordered){ordered.forEach{parent.removeView(it)};ordered.forEach{parent.addView(it)}}
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
