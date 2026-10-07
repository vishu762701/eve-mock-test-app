package com.eve.app.ui.admin.uistudio

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.repository.UiStudioRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.uistudio.*
import com.eve.app.util.UiStudioEngine
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Tool-first editor. Drafts are only ever passed to the sandbox renderer. */
class UiStudioActivity : EveBaseActivity() {
    private val repo by lazy { UiStudioRepository.getInstance() }
    private var config=UiStudioConfig()
    private var screen="home"
    private var selected=""
    private var selectedInstance: View?=null
    private var tool=0
    private var visualScope="element"
    private var loading=true
    private var menu=true
    private var previewMode=false
    private var scale=1f
    private var content: View?=null
    private var elements=emptyList<StudioRenderer.Element>()
    private lateinit var page: LinearLayout
    private lateinit var status: TextView
    private lateinit var selection: TextView
    private lateinit var preview: StudioPreview
    private var sheet: BottomSheetDialog?=null
    private var colorDialog:AlertDialog?=null
    private var busy=false
    private var fieldJob: Job?=null
    private var pendingField: (() -> Unit)?=null
    private fun flushFields() { fieldJob?.cancel();pendingField?.invoke();pendingField=null }
    private val undo=ArrayDeque<UiStudioConfig>()
    private val redo=ArrayDeque<UiStudioConfig>()
    private val tools=listOf("Glass & Materials","Colors","Text","Size & Spacing","Shape & Depth","Icons / Images","Arrange","States","Motion","Theme / Branding","Presets","Advanced")
    private val prefs by lazy { getSharedPreferences("studio_editor_history",MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo.loadSessionState()?.let { screen=it.selectedScreenKey;selected=it.selectedComponentKey;tool=(it.selectedTab.toIntOrNull() ?: 0).coerceIn(tools.indices);previewMode=it.viewMode=="preview" }
        if(prefs.getInt("controlsVersion",0)<2){tool=listOf(0,1,2,3,4,5,6,6,8,7,9,10)[tool];prefs.edit().putInt("controlsVersion",2).apply()}
        if(StudioScreens.all.none{it.key==screen})screen="home"
        scale=prefs.getFloat("scale",1f).coerceIn(.5f,2f)
        restoreHistory()
        showMenu()
        lifecycleScope.launch {
            repo.getDraft().onSuccess { config=it }.onFailure { config=repo.loadCachedDraft() ?: UiStudioConfig() }
            loading=false
            if(savedInstanceState?.getBoolean("workspace") == true)showWorkspace()
            status.text="Recoverable local draft · not published"
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        flushFields();persist();outState.putBoolean("workspace",!menu);super.onSaveInstanceState(outState)
    }
    override fun onPause() { colorDialog?.dismiss();colorDialog=null;flushFields();persist(); super.onPause() }
    private fun persist() {
        if (loading) return
        repo.saveDraftToLocalCache(config)
        repo.saveSessionState(UiStudioSessionState(selectedScreenKey=screen,selectedComponentKey=selected,selectedTab=tool.toString(),viewMode=if(previewMode)"preview" else "edit"))
        prefs.edit().putFloat("scale",scale).putString("undo",Gson().toJson(undo.toList())).putString("redo",Gson().toJson(redo.toList())).apply()
    }
    private fun restoreHistory() {
        runCatching { Gson().fromJson(prefs.getString("undo","[]"),Array<UiStudioConfig>::class.java).takeLast(30).forEach { undo.addLast(it) } }
        runCatching { Gson().fromJson(prefs.getString("redo","[]"),Array<UiStudioConfig>::class.java).takeLast(30).forEach { redo.addLast(it) } }
    }
    private fun dp(v: Int)=UiStudioEngine.dpToPx(this,v)
    private fun column()=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(4),dp(12),dp(8)) }
    private fun label(parent: LinearLayout,text: String): TextView=TextView(this).apply { this.text=text;textSize=15f;setPadding(0,dp(6),0,dp(6));parent.addView(this) }
    private fun button(parent: LinearLayout,text: String,click: ()->Unit): Button=Button(this).apply { this.text=text;isAllCaps=false;minHeight=dp(48);setOnClickListener { click() };parent.addView(this,LinearLayout.LayoutParams(-1,-2)) }
    private fun row(parent: LinearLayout,buttons: List<Pair<String,()->Unit>>) {
        val r=LinearLayout(this);parent.addView(r)
        buttons.forEach { (text,action)->Button(this).apply { this.text=text;isAllCaps=false;minHeight=dp(48);setOnClickListener { action() };r.addView(this,LinearLayout.LayoutParams(0,-2,1f)) } }
    }
    private fun initPage(title: String) {
        page=column();setContentView(page)
        label(page,title).textSize=24f
        status=label(page,if(loading) "Loading draft…" else "Local draft · preview only")
    }
    private fun showMenu() {
        flushFields();menu=true;sheet?.dismiss();initPage("UI Studio")
        label(page,"Choose a tool, then select an item on the screen. Changes stay in your draft until you publish.")
        val list=column();page.addView(ScrollView(this).apply { addView(list) },LinearLayout.LayoutParams(-1,0,1f))
        tools.forEachIndexed { index,title -> button(list,title) { if(!loading) { if(index==11)advanced() else {tool=index;visualScope="element";showWorkspace()} } } }
        row(page,listOf("Back" to { finish() },"Advanced" to { advanced() }))
    }
    private fun showWorkspace() {
        flushFields();menu=false;initPage(tools[tool]);
        row(page,listOf("Tools" to { showMenu() },"Controls" to { controls() },"Elements" to { elementList() }))
        val spinner=Spinner(this)
        val screens=StudioScreens.all
        spinner.contentDescription="Screen"
        spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,screens.map { it.label })
        spinner.setSelection(screens.indexOfFirst { it.key==screen }.coerceAtLeast(0),false)
        page.addView(spinner)
        spinner.onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p: AdapterView<*>?) {}
            override fun onItemSelected(p: AdapterView<*>?,v: View?,position: Int,id: Long) {
                if(screen!=screens[position].key) { flushFields();screen=screens[position].key;selected="";renderScreen() }
            }
        }
        selection=label(page,"Select an item · scope: selected item")
        preview=StudioPreview(this).apply {
            editing=!previewMode
            candidates={ elements }
            onSelect={ view -> selectedInstance=view;this@UiStudioActivity.selected=elements.firstOrNull { it.view===view }?.id ?: "";select();persist() }
        }
        val scroll=ScrollView(this).apply { isFillViewport=false;addView(HorizontalScrollView(this@UiStudioActivity).apply { addView(preview,ViewGroup.LayoutParams(-2,-2)) }) }
        page.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        row(page,listOf("−" to { zoom(-.15f) },"Fit" to { scale=(resources.displayMetrics.widthPixels.toFloat()/dp(384)).coerceAtMost(1f);resizePreview() },"+" to { zoom(.15f) },"Edit / Preview" to { previewMode=!previewMode;preview.editing=!previewMode;preview.invalidate();status.text=if(previewMode) "Sandbox preview · no real submissions or account actions" else "Edit mode · tap to select" }))
        row(page,listOf("Undo" to { undo() },"Redo" to { redo() }))
        row(page,listOf("Save draft" to { save() },"Publish" to { publish() }))
        renderScreen()
    }
    private fun renderScreen() {
        val descriptor=StudioScreens.all.firstOrNull { it.key==screen } ?: return
        selectedInstance=null;preview.removeAllViews()
        content=layoutInflater.inflate(descriptor.layout,preview,false)
        preview.addView(content,FrameLayout.LayoutParams(dp(360),dp(700)))
        StudioFixtures.bind(content!!,screen,{config}) { destination ->
            if(previewMode) { screen=destination;selected="";showWorkspace() }
        }
        content!!.viewTreeObserver.addOnGlobalLayoutListener { if(content?.isAttachedToWindow==true) applyPreview() }
        applyPreview();resizePreview();persist()
    }
    private fun applyPreview() {
        val root=content ?: return
        elements=StudioRenderer.apply(root,screen,config,sandbox = { a ->
            if(a.actionType=="navigate" && a.actionTarget in StudioPolicy.destinations) {
                screen=a.actionTarget!!;selected="";showWorkspace()
            } else if(a.actionType=="open_url") AlertDialog.Builder(this).setTitle("Sandbox link").setMessage("Would open ${a.actionTarget}. No external app was opened.").setPositiveButton("Close",null).show()
        })
        select()
    }
    private fun zoom(delta: Float) { scale=(scale+delta).coerceIn(.5f,2f);resizePreview() }
    private fun resizePreview() {
        content?.apply { UiStudioEngine.cancelMotion(this);pivotX=0f;pivotY=0f;scaleX=scale;scaleY=scale }
        preview.layoutParams=FrameLayout.LayoutParams((dp(360)*scale).toInt(),(dp(700)*scale).toInt())
    }
    private fun select() {
        val e=elements.firstOrNull { it.id==selected && it.view===selectedInstance } ?: elements.firstOrNull { it.id==selected }
        preview.selected=e?.view
        val label=if(e==null) "No item selected · tap preview or Elements" else "${e.parent?.removePrefix("native_")?.plus(" › ") ?: ""}${e.label}\nScope: ${if(visualScope=="screen")"selected screen" else "selected element"}${if(e.repeated) " template (all rows)" else ""}"
        if(selection.text.toString()!=label)selection.text=label
        preview.invalidate()
    }
    private fun elementList() {
        val unique=elements.distinctBy { it.id }
        val names=unique.map { "${it.label}${if(it.view.visibility!=View.VISIBLE) " · hidden" else ""}" }
        AlertDialog.Builder(this).setTitle("Elements · nested / hidden / repeated").setItems(names.toTypedArray()) { _,i -> selectedInstance=unique[i].view;selected=unique[i].id;select();controls() }.setNeutralButton("Deselect") { _,_->selected="";select() }.show()
    }
    private fun current(): ComponentConfig = elements.firstOrNull { it.id==selected }?.let { StudioRenderer.configuration(it,config.screens[screen]?.components.orEmpty()) ?: StudioRenderer.defaults(it) } ?: ComponentConfig(id=selected)
    private fun checkpoint() { if(undo.size==30)undo.removeFirst();undo.addLast(config);redo.clear() }
    private fun change(next: UiStudioConfig,history: Boolean=true) {
        if(next==config)return
        if(history)checkpoint()
        config=next.copy(status="draft");if(history)persist() else repo.saveDraftToLocalCache(config);applyPreview();status.text="Unsaved server changes · recoverable locally"
    }
    private fun edit(history: Boolean=true,block: (ComponentConfig)->ComponentConfig) {
        if(!StudioPolicy.visuallyEditable(selected))return
        val s=config.screens[screen] ?: ScreenConfig(id=screen)
        if(visualScope=="screen" && tool!=6) {
            val eligible=elements.distinctBy{it.id}.filter { when(tool){0,10->StudioMaterial.supported(it.view);2->it.view is TextView;5->it.view is ImageView || it.view is com.google.android.material.button.MaterialButton;else->true} }
            val changes=eligible.associate { e-> e.id to block(StudioRenderer.configuration(e,s.components) ?: StudioRenderer.defaults(e)) }
            change(config.copy(screens=config.screens+(screen to s.copy(components=s.components+changes))),history)
        } else change(config.copy(screens=config.screens+(screen to s.copy(components=s.components+(selected to block(current()))))),history)
    }
    private fun undo() { flushFields();if(undo.isEmpty())return;redo.addLast(config);config=undo.removeLast().copy(revision=config.revision);persist();applyPreview();status.text="Undo · local draft" }
    private fun redo() { flushFields();if(redo.isEmpty())return;undo.addLast(config);config=redo.removeLast().copy(revision=config.revision);persist();applyPreview();status.text="Redo · local draft" }
    private fun controls() {
        flushFields();sheet?.dismiss()
        val dialog=BottomSheetDialog(this);sheet=dialog
        val box=column();label(box,tools[tool]).textSize=22f
        label(box,"SCREEN: ${screen.replace('_',' ')} · SELECTED: ${elements.firstOrNull{it.id==selected}?.label ?: "none"}")
        button(box,"Scope: ${if(tool==9)"selected screen / explicit global palette" else if(visualScope=="screen")"Selected screen" else "Selected element"}") {
            choiceDialog("Apply visual controls to",listOf("Selected element","Selected screen · matching visual elements","Global palette · Theme / Branding")){value->
                if(value.startsWith("Global")){tool=9;visualScope="element";showWorkspace()}else{visualScope=if(value.startsWith("Selected screen"))"screen" else "element";select();controls()}
            }
        }
        if(visualScope=="screen")label(box,"Explicit screen scope: affects matching visible/hidden template elements on this screen. Actions and business data stay protected.")
        if(selected.isBlank() && tool !in listOf(6,9,10)) {
            label(box,"Select an item from the preview or Elements first.")
        } else buildControls(box)
        button(box,"Done") { dialog.dismiss() }
        dialog.setContentView(ScrollView(this).apply { addView(box) });dialog.show();dialog.behavior.peekHeight=dp(240)
    }
    private fun textField(box: LinearLayout,title: String,value: String?,apply: (String?)->Unit) {
        label(box,title)
        val input=EditText(this).apply { setText(value.orEmpty());if(title.startsWith("Height"))inputType=InputType.TYPE_CLASS_NUMBER;isSingleLine=true;minHeight=dp(48);contentDescription=title }
        box.addView(input)
        val screenAtEdit=screen;val itemAtEdit=selected
        var job: Job?=null
        input.doAfterTextChanged { editable ->
            val valueNow=editable?.toString()?.trim()?.ifEmpty { null }
            if(fieldJob !== job) { fieldJob?.cancel();pendingField?.invoke();pendingField=null }
            job?.cancel()
            pendingField={ if(screen==screenAtEdit && selected==itemAtEdit)apply(valueNow) }
            job=lifecycleScope.launch { delay(400);pendingField?.invoke();pendingField=null }
            fieldJob=job
        }
    }
    private fun color(box: LinearLayout,title: String,value: String?,apply: (String?)->Unit) {
        var latest=value
        val caption=label(box,"$title · ${value ?: "Production baseline"}")
        lateinit var pick:Button
        fun choose(hex:String?){latest=hex;apply(hex);UiStudioEngine.parseColorSafe(hex)?.let{pick.backgroundTintList=android.content.res.ColorStateList.valueOf(it);pick.setTextColor(if(Color.red(it)+Color.green(it)+Color.blue(it)>420)Color.BLACK else Color.WHITE)};caption.text="$title · ${hex ?: "Production baseline"}";if(hex!=null){val recent=(listOf(hex)+prefs.getString("recentColors","").orEmpty().split(',')).filter{it.isNotBlank()}.distinct().take(6);prefs.edit().putString("recentColors",recent.joinToString(",")).apply()}}
        pick=button(box,"Pick $title"){colorDialog=StudioColorPicker.show(this,latest){choose(it)}}
        UiStudioEngine.parseColorSafe(value)?.let { pick.backgroundTintList=android.content.res.ColorStateList.valueOf(it);pick.setTextColor(if(Color.red(it)+Color.green(it)+Color.blue(it)>420)Color.BLACK else Color.WHITE) }
        val colors=(prefs.getString("recentColors","").orEmpty().split(',')+listOf("#FFFFFF","#000000","#007AFF","#34C759","#FF9500","#AF52DE")).filter{it.isNotBlank()}.distinct().take(12)
        colors.chunked(6).forEach { group->val palette=LinearLayout(this);box.addView(palette);group.forEach { hex->Button(this).apply{backgroundTintList=android.content.res.ColorStateList.valueOf(Color.parseColor(hex));contentDescription="$title $hex";setOnClickListener{choose(hex)};palette.addView(this,LinearLayout.LayoutParams(0,dp(48),1f))} } }
        row(box,listOf("HEX…" to { val input=EditText(this).apply{setText(latest.orEmpty());hint="#RRGGBB or #AARRGGBB"};AlertDialog.Builder(this).setTitle(title).setView(input).setPositiveButton("Apply"){_,_->val hex=input.text.toString().trim().uppercase();if(UiStudioEngine.parseColorSafe(hex)!=null)choose(if(hex.startsWith("#"))hex else "#$hex")else status.text="Invalid HEX color"}.setNegativeButton("Cancel",null).show() },"Reset" to {choose(null)}))
    }
    private fun slider(box: LinearLayout,title: String,value: Int,min: Int,max: Int,apply: (Int,Boolean)->Unit) {
        val caption=label(box,"$title: $value")
        val seek=SeekBar(this).apply { this.max=max-min;progress=value.coerceIn(min,max)-min;contentDescription=title;minimumHeight=dp(48) }
        box.addView(seek)
        row(box,listOf("− 1" to {val v=(seek.progress+min-1).coerceAtLeast(min);seek.progress=v-min;caption.text="$title: $v";apply(v,true)},"+ 1" to {val v=(seek.progress+min+1).coerceAtMost(max);seek.progress=v-min;caption.text="$title: $v";apply(v,true)}))
        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            private var dragging=false
            override fun onStartTrackingTouch(s: SeekBar?) { checkpoint();dragging=true }
            override fun onStopTrackingTouch(s: SeekBar?) { dragging=false;persist() }
            override fun onProgressChanged(s: SeekBar?,p: Int,user: Boolean) { if(user) { caption.text="$title: ${p+min}";apply(p+min,!dragging) } }
        })
    }
    private fun toggle(box: LinearLayout,title: String,value: Boolean,apply: (Boolean)->Unit) {
        box.addView(androidx.appcompat.widget.SwitchCompat(this).apply { text=title;isChecked=value;minHeight=dp(48);setOnCheckedChangeListener { _,checked->apply(checked) } })
    }
    private fun choice(box: LinearLayout,title: String,values: List<String>,apply: (String)->Unit) {
        button(box,title) { AlertDialog.Builder(this).setTitle(title).setItems(values.toTypedArray()) { _,i->apply(values[i]) }.show() }
    }
    private fun buildControls(box: LinearLayout) {
        val c=current();val e=elements.firstOrNull { it.id==selected };val dynamic=StudioPolicy.isDynamic(selected)
        when(tool) {
            0 -> {
                val supported=StudioMaterial.supported(e?.view)
                label(box,if(supported) "Android backdrop blur · foreground stays sharp. Uses BlurView on API 24+. No refraction or morphing. Check text contrast; clear surfaces need a suitable backdrop." else "This native layout cannot host backdrop blur safely. Select a containing card, bar, panel or button in a supported parent. Tint here is a flat fill approximation.")
                if(!supported)button(box,"Select containing surface") { val parent=generateSequence(e?.view?.parent as? View){it.parent as? View}.firstOrNull{StudioMaterial.supported(it)};elements.firstOrNull{it.view===parent}?.let{selected=it.id;selectedInstance=it.view;select();controls()} }
                if(supported) slider(box,"Backdrop blur radius",c.material.blurRadius ?: 0,0,25) { v,h->edit(h){it.copy(material=it.material.copy(blurRadius=v))} }
                slider(box,"Material opacity %",((c.material.materialOpacity ?: 1f)*100).toInt(),0,100) { v,h->edit(h){it.copy(material=it.material.copy(materialOpacity=v/100f))} }
                color(box,"Tint",c.material.tintColor) { v->edit{it.copy(material=it.material.copy(tintColor=v))} }
                slider(box,"Tint opacity %",((c.material.tintOpacity ?: .2f)*100).toInt(),0,100) { v,h->edit(h){it.copy(material=it.material.copy(tintOpacity=v/100f))} }
                if(supported) choice(box,"Material preset",listOf("Clear","Tinted","Frosted","Opaque accessibility")) { name ->
                    val neutral=if((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES)"#000000" else "#FFFFFF"
                    edit { it.copy(material=when(name) { "Clear"->MaterialProperties(6,1f,neutral,.08f);"Tinted"->MaterialProperties(12,1f,"#007AFF",.25f);"Frosted"->MaterialProperties(20,1f,neutral,.6f);else->MaterialProperties(0,1f,neutral,1f) }) };controls()
                }
                button(box,"Reset material") { edit { it.copy(material=MaterialProperties()) };controls() }
            }
            1 -> {
                if((c.material.blurRadius ?: 0)>0)label(box,"An opaque background fill covers the backdrop. Use a translucent HEX fill or Reset fill for visible glass; material tint stays independent.")
                color(box,"Item background",c.appearance.backgroundColor) { v->edit{it.copy(appearance=it.appearance.copy(backgroundColor=v))} }
                slider(box,"Whole item opacity %",((c.appearance.opacity ?: e?.view?.alpha ?: 1f)*100).toInt(),0,100) { v,h->edit(h){it.copy(appearance=it.appearance.copy(opacity=v/100f))} }
                if(!dynamic)label(box,"Fully transparent native controls retain their actions. Keep essential login, submit and navigation controls discoverable; use Reset to restore them.")
                button(box,"Reset colors") { edit { it.copy(appearance=it.appearance.copy(backgroundColor=null,opacity=null)) };controls() }
            }
            2 -> {
                if(e?.view !is TextView)label(box,"Choose a child text element to edit typography.") else {
                    color(box,"Text color",c.typography.textColor){v->edit{it.copy(typography=it.typography.copy(textColor=v))}}
                    slider(box,"Text size (sp)",c.typography.textSize ?: ((e.view as TextView).textSize/resources.displayMetrics.scaledDensity).toInt(),10,48){v,h->edit(h){it.copy(typography=it.typography.copy(textSize=v))}}
                    choice(box,"Font family",listOf("sans-serif","serif","monospace")){v->edit{it.copy(typography=it.typography.copy(fontFamily=v))}}
                    choice(box,"Font style",listOf("normal","bold","italic","bold_italic")){v->edit{it.copy(typography=it.typography.copy(textStyle=v))}}
                    choice(box,"Text alignment",listOf("start","center","end")){v->edit{it.copy(typography=it.typography.copy(textAlign=v))}}
                    button(box,"Reset text styling"){edit{it.copy(typography=TypographyProperties())};controls()}
                }
            }
            3 -> {
                label(box,"Native position is constrained by the production layout. Inserted items can be arranged in their container.")
                slider(box,"Top spacing (dp)",c.layout.marginTop ?: (((e?.view?.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0)/resources.displayMetrics.density).toInt(),0,64){v,h->edit(h){it.copy(layout=it.layout.copy(marginTop=v))}}
                slider(box,"Bottom spacing (dp)",c.layout.marginBottom ?: (((e?.view?.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0)/resources.displayMetrics.density).toInt(),0,64){v,h->edit(h){it.copy(layout=it.layout.copy(marginBottom=v))}}
                slider(box,"Inner padding (dp)",c.layout.paddingTop ?: ((e?.view?.paddingTop ?: 0)/resources.displayMetrics.density).toInt(),0,48){v,h->edit(h){it.copy(layout=it.layout.copy(paddingTop=v,paddingBottom=v,paddingStart=v,paddingEnd=v))}}
                if(dynamic || e?.view is ImageView || e?.view is Button)textField(box,"Width (48–600 dp, blank = baseline)",c.layout.width){v->if(v==null || v.toIntOrNull() in 48..600)edit{it.copy(layout=it.layout.copy(width=v))}}
                if(dynamic || e?.view is ViewGroup || e?.view is ImageView || e?.view is Button) textField(box,"Height (48–600 dp, blank = baseline)",c.layout.height){v->if(v==null || v.toIntOrNull() in 48..600)edit{it.copy(layout=it.layout.copy(height=v))}}
                button(box,"Reset layout"){edit{it.copy(layout=LayoutProperties())};controls()}
            }
            4 -> {
                choice(box,"Shape: ${c.appearance.shape ?: "rounded"}",listOf("rounded","capsule","circle")){v->edit{it.copy(appearance=it.appearance.copy(shape=v))}}
                label(box,"Capsule uses half the short edge. Circle requires equal width and height. Rounded Android outlines approximate continuous corners.")
                slider(box,"Corner radius (dp)",c.appearance.cornerRadius ?: 0,0,60){v,h->edit(h){it.copy(appearance=it.appearance.copy(cornerRadius=v))}}
                slider(box,"Border width (dp)",c.appearance.strokeWidth ?: 0,0,8){v,h->edit(h){it.copy(appearance=it.appearance.copy(strokeWidth=v))}}
                color(box,"Border / subtle edge highlight",c.appearance.strokeColor){v->edit{it.copy(appearance=it.appearance.copy(strokeColor=v))}}
                slider(box,"Elevation (dp)",c.appearance.elevation ?: 0,0,24){v,h->edit(h){it.copy(appearance=it.appearance.copy(elevation=v))}}
                color(box,"Layered edge highlight",c.appearance.highlightColor){v->edit{it.copy(appearance=it.appearance.copy(highlightColor=v))}}
                slider(box,"Highlight strength %",((c.appearance.highlightOpacity ?: .35f)*100).toInt(),0,100){v,h->edit(h){it.copy(appearance=it.appearance.copy(highlightOpacity=v/100f))}}
                button(box,"Reset shape"){edit{it.copy(appearance=it.appearance.copy(cornerRadius=null,strokeWidth=null,strokeColor=null,elevation=null,shape=null,highlightColor=null,highlightOpacity=null))};controls()}
            }
            5 -> {
                if(e?.view is ImageView || e?.view is com.google.android.material.button.MaterialButton || (e?.view as? TextView)?.compoundDrawables?.any{it!=null}==true) {
                    color(box,"Icon / image tint",c.appearance.iconTint){v->edit{it.copy(appearance=it.appearance.copy(iconTint=v))}}
                    button(box,"Reset icon tint"){edit{it.copy(appearance=it.appearance.copy(iconTint=null))};controls()}
                }
                if(!dynamic && selected in setOf("native_tvLogo","native_tvAppName")) textField(box,"Static app label",c.content.title){v->edit{it.copy(content=it.content.copy(title=v))}}
                else if(!dynamic)label(box,"Native content is owned by its data binding. Scores, exam titles, questions and timers are never replaced by preview fixtures. Insert a text/image element for static content.")
                else when(c.type) {
                    "image"->textField(box,"Image HTTP(S) URL",c.content.imageSource){v->if(v==null || StudioPolicy.safeUrl(v))edit{it.copy(content=it.content.copy(imageSource=v))}else status.text="Invalid image URL"}
                    "icon"->choice(box,"Icon",listOf("star","info","mail")){v->edit{it.copy(content=it.content.copy(icon=v))}}
                    "text","button"->textField(box,"Static label",c.content.title){v->edit{it.copy(content=it.content.copy(title=v))}}
                    else->label(box,"Select or add a child text/image element.")
                }
            }
            6 -> { arrange(box,dynamic);actionControls(box,dynamic,c) }
            8 -> {
                label(box,"Item entrance only; optical morphing and refraction are unsupported. Screen entrance is a separate explicit scope.")
                toggle(box,"Selected screen entrance",config.screens[screen]?.animation?.enabled ?: false){v->val s=config.screens[screen] ?: ScreenConfig(id=screen);change(config.copy(screens=config.screens+(screen to s.copy(animation=s.animation.copy(enabled=v)))))}
                toggle(box,"Entrance motion (respects system animation scale)",c.animation.enabled){v->edit{it.copy(animation=it.animation.copy(enabled=v))}}
                choice(box,"Motion: ${c.animation.type}",listOf("fade","scale","fade_scale","slide")){v->edit{it.copy(animation=it.animation.copy(type=v))}}
                slider(box,"Duration (ms)",c.animation.durationMs.toInt(),50,2000){v,h->edit(h){it.copy(animation=it.animation.copy(durationMs=v.toLong()))}}
                choice(box,"Response curve: ${c.animation.interpolator}",listOf("standard","ease_in","ease_out","spring")){v->edit{it.copy(animation=it.animation.copy(interpolator=v))}}
                label(box,"Press styling observes native state and preserves click/touch actions. Spring is an Android overshoot approximation.")
                slider(box,"Pressed scale %",((c.animation.pressScale ?: 1f)*100).toInt(),85,100){v,h->edit(h){it.copy(animation=it.animation.copy(pressScale=v/100f))}}
                toggle(box,"Spring-like release",c.animation.springRelease){v->edit{it.copy(animation=it.animation.copy(springRelease=v))}}
                toggle(box,"Subtle press haptic (system setting respected)",c.animation.hapticFeedback){v->edit{it.copy(animation=it.animation.copy(hapticFeedback=v))}}
                button(box,"Replay selected entrance"){e?.view?.let { UiStudioEngine.playEntranceAnimation(it,current().animation) }}
                button(box,"Reset motion"){edit{it.copy(animation=AnimationProperties())};controls()}
            }
            7 -> {
                color(box,"Pressed background",c.states.pressedBackgroundColor){v->edit{it.copy(states=it.states.copy(pressedBackgroundColor=v))}}
                color(box,"Selected background",c.states.selectedBackgroundColor){v->edit{it.copy(states=it.states.copy(selectedBackgroundColor=v))}}
                color(box,"Disabled background",c.states.disabledBackgroundColor){v->edit{it.copy(states=it.states.copy(disabledBackgroundColor=v))}}
                color(box,"Focused background",c.states.focusedBackgroundColor){v->edit{it.copy(states=it.states.copy(focusedBackgroundColor=v))}}
                color(box,"Selected text",c.states.selectedTextColor){v->edit{it.copy(states=it.states.copy(selectedTextColor=v))}}
                slider(box,"State / material color transition (ms)",c.animation.stateTransitionMs.toInt(),0,600){v,h->edit(h){it.copy(animation=it.animation.copy(stateTransitionMs=v.toLong()))}}
                button(box,"Preview selected state"){e?.view?.isSelected=!(e?.view?.isSelected ?: false)}
                if(dynamic)toggle(box,"Enabled",c.enabled){v->edit{it.copy(enabled=v)}}
                button(box,"Reset state styling"){edit{it.copy(states=StateProperties())};controls()}
            }
            9 -> {
                label(box,"Default scope: selected screen. Global changes require explicit opt-in. Launcher name/icon remain Android package metadata.")
                color(box,"${screen.replace('_',' ')} background",config.screens[screen]?.backgroundColor){v->val s=config.screens[screen] ?: ScreenConfig(id=screen);change(config.copy(screens=config.screens+(screen to s.copy(backgroundColor=v))))}
                toggle(box,"Apply global branding labels (Login / About)",config.branding.enabled){v->change(config.copy(branding=config.branding.copy(enabled=v)));controls()}
                if(config.branding.enabled) {
                    textField(box,"Global display name",config.branding.appDisplayName){v->change(config.copy(branding=config.branding.copy(appDisplayName=v ?: "EVE Exam Prep")))}
                    textField(box,"Global short name",config.branding.shortName){v->change(config.copy(branding=config.branding.copy(shortName=v ?: "EVE")))}
                    color(box,"Global brand label color",config.branding.brandColor){v->change(config.copy(branding=config.branding.copy(brandColor=v ?: "#007AFF")))}
                }
                toggle(box,"Apply global background and text palette",config.designSystem.enabled){v->change(config.copy(designSystem=config.designSystem.copy(enabled=v)));controls()}
                if(config.designSystem.enabled) {
                    color(box,"Global app background",config.designSystem.appBackground){v->change(config.copy(designSystem=config.designSystem.copy(appBackground=v ?: "#000000")))}
                    color(box,"Global text color",config.designSystem.textPrimary){v->change(config.copy(designSystem=config.designSystem.copy(textPrimary=v ?: "#FFFFFF")))}
                    label(box,"Global palette is explicit and fixed across light/dark themes. Item text and screen colors take precedence.")
                }
                button(box,"Restore device theme and native branding") { change(config.copy(branding=config.branding.copy(enabled=false),designSystem=config.designSystem.copy(enabled=false)));controls() }
            }
            10 -> {
                if(StudioMaterial.supported(e?.view))choice(box,"Glass combination shortcut",StudioPresets.names){name->edit{StudioPresets.apply(it,name,(resources.configuration.uiMode and 0x30)==0x20)};controls()}
                else label(box,"Select a glass-capable containing surface for combination shortcuts. All manual visual controls remain separate.")
                button(box,"Reset selected item to native baseline") { if(selected.isNotBlank()){val s=config.screens[screen] ?: return@button;change(config.copy(screens=config.screens+(screen to s.copy(components=s.components-run { val ids=StudioRenderer.keysFor(selected).toMutableSet();repeat(s.components.size){s.components.values.filter{it.parentId in ids}.forEach{ids+=it.id}};ids }))))};controls() }
                button(box,"Reset selected screen") { confirm("Restore native styling for $screen? Inserted items on this screen will be removed from the draft.") { change(config.copy(screens=config.screens-screen));controls() } }
                button(box,"Discard draft edits to last published") { confirm("Replace this draft with the cached published configuration? Undo remains available.") { change(repo.currentConfig.copy(revision=config.revision));controls() } }
                button(box,"Version history / restore") { versions() }
                label(box,"Material-only presets are in Glass & Materials and affect only the selected material. Resets remain drafts until published.")
            }
        }
    }
    private fun actionControls(box:LinearLayout,dynamic:Boolean,c:ComponentConfig) {
                if(!dynamic)label(box,"Native navigation, login and exam actions are protected. Preview uses local representative interactions only.") else {
                    choice(box,"Action: ${c.actions.actionType}",listOf("none","navigate","open_url")){v->edit{it.copy(actions=ActionProperties(v,if(v=="navigate")"home" else null))};controls()}
                    if(c.actions.actionType=="navigate")choice(box,"Destination: ${c.actions.actionTarget}",StudioPolicy.destinations.toList()){v->edit{it.copy(actions=it.actions.copy(actionTarget=v))};controls()}
                    if(c.actions.actionType=="open_url")textField(box,"HTTP(S) destination",c.actions.actionTarget){v->if(StudioPolicy.safeUrl(v))edit{it.copy(actions=it.actions.copy(actionTarget=v))}}
                }
    }
    private fun arrange(box: LinearLayout,dynamic: Boolean) {
        label(box,"Native controls and their ancestors keep their app-owned structure, visibility and actions. Inserted components use a scrollable region capped at one third of device height to preserve native content. Use Preview mode to scroll inside it.")
        button(box,"Add component") {
            if(content?.let { StudioRenderer.canInsert(it) }!=true){status.text="This screen has no safe insertion container";return@button}
            choiceDialog("Add to screen's content container",StudioPolicy.dynamicTypes){type->
                val id="custom_"+UUID.randomUUID().toString().replace("-","")
                val s=config.screens[screen] ?: ScreenConfig(id=screen)
                val c=ComponentConfig(id=id,name="New $type",type=type,order=(s.components.values.maxOfOrNull{it.order} ?: 0)+1,layout=LayoutProperties(height=if(type=="spacer")"16" else if(type=="divider")"1" else if(type in listOf("image","icon"))"96" else if(type in listOf("card","banner"))"120" else null,paddingTop=8,paddingBottom=8),content=ContentProperties(title=if(type in listOf("text","button"))"New $type" else null),appearance=AppearanceProperties(backgroundColor=if(type=="divider")"#808080" else if(type in listOf("card","banner"))if((resources.configuration.uiMode and 0x30)==0x20)"#1C1C1E" else "#F2F2F7" else null,cornerRadius=if(type in listOf("card","banner"))16 else null))
                selected=id;change(config.copy(screens=config.screens+(screen to s.copy(components=s.components+(id to c)))));controls()
            }
        }
        if(!dynamic)return
        val c=current()
        toggle(box,"Visible",c.visible){v->edit{it.copy(visible=v)}}
        button(box,"Duplicate") {
            val s=config.screens[screen] ?: return@button
            val subtree=mutableSetOf(c.id)
            repeat(s.components.size) { s.components.values.filter { it.parentId in subtree }.forEach { subtree+=it.id } }
            val ids=subtree.associateWith { "custom_"+UUID.randomUUID().toString().replace("-","") }
            val copies=subtree.associate { old -> val item=s.components.getValue(old);val id=ids.getValue(old);id to item.copy(id=id,name=item.name+" copy",parentId=ids[item.parentId] ?: item.parentId,order=item.order+1) }
            selected=ids.getValue(c.id);change(config.copy(screens=config.screens+(screen to s.copy(components=s.components+copies))));controls()
        }
        row(box,listOf("Move up" to { reorder(-1) },"Move down" to { reorder(1) }))
        button(box,"Choose parent") {
            val s=config.screens[screen] ?: return@button
            val parents=listOf("Screen container")+s.components.values.filter{it.id!=selected && StudioPolicy.isDynamic(it.id) && it.type in listOf("card","banner")}.map{it.id}
            choiceDialog("Parent",parents){value->
                val candidate=c.copy(parentId=value.takeIf{it!="Screen container"})
                val next=config.copy(screens=config.screens+(screen to s.copy(components=s.components+(selected to candidate))))
                val errors=StudioPolicy.validate(next)
                if(errors.isEmpty())change(next)else status.text=errors.first()
            }
        }
        button(box,"Remove item and inserted children") {
            val s=config.screens[screen] ?: return@button
            val remove=mutableSetOf(selected)
            repeat(s.components.size){s.components.values.filter{it.parentId in remove}.forEach{remove+=it.id}}
            change(config.copy(screens=config.screens+(screen to s.copy(components=s.components-remove))));selected="";controls()
        }
    }
    private fun reorder(delta: Int) {
        val s=config.screens[screen] ?: return
        val c=current();val siblings=s.components.values.filter{StudioPolicy.isDynamic(it.id)&&it.parentId==c.parentId}.sortedWith(compareBy({it.order},{it.id})).toMutableList()
        val i=siblings.indexOfFirst{it.id==selected};val j=i+delta
        if(i<0 || j !in siblings.indices)return
        java.util.Collections.swap(siblings,i,j)
        change(config.copy(screens=config.screens+(screen to s.copy(components=s.components+siblings.mapIndexed{index,item->item.id to item.copy(order=index)}.toMap()))))
    }
    private fun choiceDialog(title: String,values: List<String>,action: (String)->Unit) { AlertDialog.Builder(this).setTitle(title).setItems(values.toTypedArray()){_,i->action(values[i])}.show() }
    private fun confirm(message: String,action: ()->Unit) { AlertDialog.Builder(this).setMessage(message).setPositiveButton("Continue"){_,_->action()}.setNegativeButton("Cancel",null).show() }
    private fun save() {
        flushFields();if(busy)return
        val errors=repo.validateConfig(config).second
        if(errors.isNotEmpty()){status.text=errors.joinToString("\n");return}
        busy=true;val snapshot=config
        lifecycleScope.launch {
            when(val result=repo.saveDraftDetailed(snapshot)) {
                is SaveDraftResult.ServerSuccess -> { if(config==snapshot)config=result.config else config=config.copy(revision=result.config.revision);persist();status.text=if(config.screens==snapshot.screens)"Saved on server · not published" else "Server saved earlier edit · newer edits local" }
                is SaveDraftResult.LocalOfflineSuccess -> status.text="Saved locally/offline · ${result.error}"
                is SaveDraftResult.Conflict -> { status.text="Conflict · local work kept";conflict(result.serverDraft) }
                is SaveDraftResult.Failure -> status.text="Save rejected · local draft retained: ${result.error}"
            }
            busy=false
        }
    }
    private fun conflict(server: UiStudioConfig?) {
        AlertDialog.Builder(this).setTitle("Newer server draft").setMessage("Your draft is kept locally. Export it from Advanced before loading the server copy. Overwrite is disabled.").setPositiveButton("Keep local",null).setNeutralButton("Load server") { _,_-> lifecycleScope.launch { val remote=server ?: repo.getServerDraft().getOrNull();if(remote!=null) { change(remote);status.text="Loaded server draft · previous local edits remain in Undo" } } }.show()
    }
    private fun publish() {
        flushFields();if(busy)return
        confirm("Publish this draft to students? The server revision and fresh readback must match.") {
            val errors=repo.validateConfig(config).second
            if(errors.isNotEmpty()){status.text=errors.joinToString("\n");return@confirm}
            busy=true;val snapshot=config
            lifecycleScope.launch {
                when(val result=repo.publishVerified("UI Studio editor",snapshot)) {
                    is PublishResult.VerifiedSuccess -> { if(config==snapshot)config=result.config else config=config.copy(revision=result.config.revision);persist();status.text="Published v${result.version} · fields verified by fresh readback" }
                    is PublishResult.VerificationFailed -> status.text="Publish readback failed · ${result.reason}"
                    is PublishResult.NetworkFailure -> status.text="Publish failed · ${result.error}"
                }
                busy=false
            }
        }
    }
    private fun versions() { lifecycleScope.launch { repo.getVersions().onSuccess { list->choiceDialog("Restore to draft (not live)",list.map{"v${it.version} · ${it.notes.orEmpty()}"}) { label->val version=list.first{label.startsWith("v${it.version} ·")};lifecycleScope.launch { repo.restoreVersion(version.version).onSuccess { repo.getServerDraft().onSuccess { change(it);status.text="Version restored to draft" } }.onFailure{status.text=it.message} } } }.onFailure{status.text=it.message} } }
    private fun advanced() {
        choiceDialog("Advanced",listOf("Export JSON","Import JSON","Audit log","Version history","Copy style","Paste style")) { action->when(action){
            "Export JSON"->{ val input=EditText(this).apply{setText(repo.exportToJson(config));setSelectAllOnFocus(true)};AlertDialog.Builder(this).setTitle("Draft JSON · select to copy").setView(input).setPositiveButton("Close",null).show() }
            "Import JSON"->{val input=EditText(this);AlertDialog.Builder(this).setTitle("Import into draft").setView(input).setPositiveButton("Validate & import"){_,_->repo.importFromJson(input.text.toString()).onSuccess { change(it.copy(revision=config.revision));if(!menu)renderScreen() }.onFailure{status.text=it.message}}.setNegativeButton("Cancel",null).show()}
            "Audit log"->lifecycleScope.launch{repo.getAuditLogs().onSuccess{logs->AlertDialog.Builder(this@UiStudioActivity).setTitle("Audit log").setMessage(logs.joinToString("\n"){"${it.action} · ${it.performedBy}"}).setPositiveButton("Close",null).show()}.onFailure{status.text=it.message}}
            "Version history"->versions()
            "Copy style"->if(selected.isNotBlank())repo.copyStyle(current())
            "Paste style"->if(selected.isNotBlank())edit{repo.pasteStyle(it)}
        } }
    }
}
