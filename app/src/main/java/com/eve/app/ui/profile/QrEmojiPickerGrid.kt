package com.eve.app.ui.profile

import android.content.Context
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.designsystem.DesignMotion
import com.eve.app.telegram.animation.NativeQrDrawable
import com.eve.app.telegram.animation.QrAnimationAssets
import com.eve.app.telegram.qr.EmojiPickOverlay
import com.eve.app.telegram.qr.QrEmojiGrid
import com.eve.app.telegram.qr.QrEmojiCell
import com.eve.app.telegram.qr.QrPickerOpenMotion
import com.eve.app.telegram.qr.QrPickerCloseCascade
import kotlinx.coroutines.*

/** EVE catalogue/lifecycle adapter for the original Java QR cells and selection overlay. */
internal class QrEmojiPickerGrid(
    context: Context,
    private val picks: () -> List<QrEmojiChoice>,
    private val toggle: (QrEmojiChoice) -> Unit,
    private val selectedDrawable: (String) -> Drawable?,
    private val loadFrame: suspend (QrEmojiChoice, Int) -> Drawable = { choice, size ->
        NativeQrDrawable.fromJson(QrAnimationAssets.json(context.applicationContext, choice.source), size)
    },
    private val tint: () -> Int = { 0x33FFFFFF }
) : FrameLayout(context) {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private var assetScope: CoroutineScope? = null
    private var sourceOpenAnimator: ValueAnimator? = null
    private var sourceCloseAnimator: ValueAnimator? = null
    private val holders = mutableSetOf<Holder>()
    private var choices = emptyList<QrEmojiChoice>()
    private var pendingScroll: android.os.Parcelable? = null
    private val manager = GridLayoutManager(context, 7)
    private val overlay: EmojiPickOverlay
    private val grid = QrEmojiGrid(context)
    private inner class Holder(val cell: QrEmojiCell) : RecyclerView.ViewHolder(cell) {
        var choice: QrEmojiChoice? = null
        var drawable: Drawable? = null
        var job: Job? = null
        var generation = 0
        fun release() {
            generation++
            job?.cancel()
            job = null
            cell.setFrameDrawable(null)
            (drawable as? AutoCloseable)?.close()
            drawable = null
        }
        fun load() {
            val scope = assetScope ?: return
            val entry = choice ?: return
            if (job != null || drawable != null) return
            val owner = generation
            job = scope.launch {
                var loaded: Drawable? = null
                try {
                    loaded = loadFrame(entry, dp(72).coerceAtMost(1024))
                    ensureActive()
                    if (generation != owner || choice != entry || !cell.isAttachedToWindow) return@launch
                    drawable = loaded
                    cell.setMotionEnabled(DesignMotion.enabled(context))
                    cell.setFrameDrawable(loaded)
                    loaded = null // holder now owns the frame and native renderer
                    overlay.invalidate()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Original placeholder remains; never substitute a Unicode animated document.
                    cell.contentDescription = "${entry.alt}, animation unavailable. Tap to select."
                } finally {
                    (loaded as? AutoCloseable)?.close()
                    if (generation == owner) job = null
                }
            }
        }
    }
    private val cells = object : RecyclerView.Adapter<Holder>() {
        override fun getItemCount() = choices.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            QrEmojiCell(context).apply {
                layoutParams = RecyclerView.LayoutParams(-1, -2)
            }
        ).also { holders.add(it) }
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.release()
            val choice = choices[position]
            holder.choice = choice
            holder.cell.setKey(choice.key)
            holder.cell.selectScale = 1f
            holder.cell.cascadeScale = 1f
            holder.cell.setMotionEnabled(DesignMotion.enabled(context))
            holder.cell.setOnClickListener {
                toggle(choice)
                holder.cell.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                updatePicks()
            }
            holder.cell.setOnLongClickListener {
                if (picks().none { it.key == choice.key }) false else {
                    toggle(choice)
                    holder.cell.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    updatePicks()
                    true
                }
            }
            describe(holder)
            if (holder.cell.isAttachedToWindow) holder.load()
        }
        override fun onViewAttachedToWindow(holder: Holder) { holder.load() }
        override fun onViewDetachedFromWindow(holder: Holder) { holder.release() }
        override fun onViewRecycled(holder: Holder) {
            holder.release()
            holder.choice = null
        }
    }

    init {
        overlay = EmojiPickOverlay(context, object : EmojiPickOverlay.Host {
            override fun pickedKeys() = picks().map { it.key }
            override fun emojiFor(key: String): Drawable? = selectedDrawable(key)
                ?: holders.firstOrNull { it.choice?.key == key }?.drawable
            override fun removePickAt(index: Int) {
                picks().getOrNull(index)?.let(toggle)
                updatePicks()
            }
            override fun islandTint() = tint()
            // Full QrShareView backdrop composition remains an explicit separate gap.
            override fun drawBlur(canvas: android.graphics.Canvas, dx: Float, dy: Float) = false
            override fun qrViewLocation(out: IntArray) { getLocationOnScreen(out) }
        }).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setMotionEnabled(DesignMotion.enabled(context))
        }
        grid.setSelectionOverlay(overlay)
        grid.layoutManager = manager
        grid.adapter = cells
        grid.itemAnimator = null
        grid.overScrollMode = View.OVER_SCROLL_NEVER
        grid.isVerticalScrollBarEnabled = false
        grid.clipToPadding = false
        addView(grid, LayoutParams(-1, -1))
        addView(overlay, LayoutParams(-1, -1))
        overlay.attach(grid)
        overlay.attachGrid(grid)
        updatePicks()
    }

    /**
     * ADAPTED source QrShareActivity.gridFades opening transform. The RecyclerView
     * uses source 420ms ease-out scale from 0.75 to 1, pivoting at the visible
     * glass bottom, with the source's 0.55 sheet-progress fade relation.
     * The outer Material sheet is not the upstream native morph compositor.
     */
    fun animateSourceOpen() {
        val pendingOpen = sourceOpenAnimator
        sourceOpenAnimator = null
        pendingOpen?.cancel()
        val pendingClose = sourceCloseAnimator
        sourceCloseAnimator = null
        pendingClose?.cancel()
        holders.forEach { holder ->
            holder.cell.cascadeScale = 1f
            holder.cell.invalidate()
        }
        if (!isAttachedToWindow || grid.width <= 0 || grid.height <= 0 ||
            !DesignMotion.enabled(context)) {
            settleSourceOpen()
            return
        }
        updateSourceOpenPivot()
        val animator = ValueAnimator.ofFloat(0f, 1f)
        sourceOpenAnimator = animator
        animator.duration = QrPickerOpenMotion.REVEAL_MS
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val scale = QrPickerOpenMotion.scale(progress)
            grid.scaleX = scale
            grid.scaleY = scale
            grid.alpha = QrPickerOpenMotion.alpha(progress)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (sourceOpenAnimator === animation) {
                    sourceOpenAnimator = null
                    settleSourceOpen()
                }
            }
        })
        grid.scaleX = QrPickerOpenMotion.scale(0f)
        grid.scaleY = grid.scaleX
        grid.alpha = QrPickerOpenMotion.alpha(0f)
        animator.start()
    }

    /** Top-first source row collapse over 150ms while the dialog fades out. */
    fun animateSourceClose() {
        val pendingOpen = sourceOpenAnimator
        sourceOpenAnimator = null
        pendingOpen?.cancel()
        val pendingClose = sourceCloseAnimator
        sourceCloseAnimator = null
        pendingClose?.cancel()
        if (!isAttachedToWindow || !DesignMotion.enabled(context)) return
        val animator = ValueAnimator.ofFloat(0f, 1f)
        sourceCloseAnimator = animator
        animator.duration = QrPickerCloseCascade.CLOSE_SPAN_MS
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { frame ->
            val t = (frame.animatedValue as Float) * QrPickerCloseCascade.CLOSE_SPAN_MS
            val n = grid.childCount
            var rowH = 0f
            for (i in 0 until n) {
                val cell = grid.getChildAt(i) as? QrEmojiCell ?: continue
                if (cell.height > 0) { rowH = cell.height.toFloat(); break }
            }
            if (rowH <= 0f) return@addUpdateListener
            val top = grid.paddingTop.toFloat()
            val rows = (grid.height - grid.paddingTop - grid.paddingBottom)
                .coerceAtLeast(1) / rowH
            for (i in 0 until n) {
                val cell = grid.getChildAt(i) as? QrEmojiCell ?: continue
                val relativeRow = (cell.top + cell.height / 2f - top) / rowH
                cell.cascadeScale = QrPickerCloseCascade.cellScale(t, relativeRow, rows)
                cell.invalidate()
            }
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (sourceCloseAnimator === animation) sourceCloseAnimator = null
            }
        })
        animator.start()
    }

    private fun updateSourceOpenPivot() {
        grid.pivotX = grid.width / 2f
        grid.pivotY = (grid.height - grid.paddingBottom).toFloat()
            .coerceIn(0f, grid.height.toFloat())
    }

    private fun settleSourceOpen() {
        grid.scaleX = 1f
        grid.scaleY = 1f
        grid.alpha = 1f
    }

    fun setItems(items: List<QrEmojiChoice>) {
        choices = items.filter { it.source.startsWith("https://") || it.source.startsWith("res:raw/") }
            .distinctBy { it.key }
        cells.notifyDataSetChanged()
        pendingScroll?.let { manager.onRestoreInstanceState(it) }
        pendingScroll = null
    }

    fun saveScrollState(): android.os.Parcelable? = pendingScroll ?: manager.onSaveInstanceState()
    fun restoreScrollState(state: android.os.Parcelable?) { pendingScroll = state }

    fun updatePicks() {
        grid.setPadding(0, 0, 0, dp(if (picks().isEmpty()) 12 else 73))
        holders.forEach {
            describe(it)
            it.cell.setMotionEnabled(DesignMotion.enabled(context))
        }
        overlay.setMotionEnabled(DesignMotion.enabled(context))
        overlay.invalidate()
        grid.invalidate()
    }

    private fun describe(holder: Holder) {
        val entry = holder.choice ?: return
        val position = picks().indexOfFirst { it.key == entry.key }
        holder.cell.isSelected = position >= 0
        holder.cell.contentDescription = if (position >= 0)
            "${entry.alt}, selected ${position + 1} of ${picks().size}. Tap to remove."
        else "${entry.alt}, animated EVE emoji. Tap to select."
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Source nominal 46dp; EVE accommodates at least 48dp accessible touch targets.
        manager.spanCount = (w / dp(48).coerceAtLeast(1)).coerceAtLeast(1)
        overlay.gridCutY(h.toFloat())
        overlay.setGlassRight(w.toFloat())
        if (sourceOpenAnimator != null) updateSourceOpenPivot()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isAttachedToWindow) updatePicks()
    }

    override fun onAttachedToWindow() {
        assetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        super.onAttachedToWindow()
        holders.filter { it.cell.isAttachedToWindow }.forEach { it.load() }
    }

    override fun onDetachedFromWindow() {
        val opening = sourceOpenAnimator
        sourceOpenAnimator = null
        opening?.cancel()
        val closing = sourceCloseAnimator
        sourceCloseAnimator = null
        closing?.cancel()
        holders.forEach { holder ->
            holder.cell.cascadeScale = 1f
            holder.cell.invalidate()
        }
        settleSourceOpen()
        assetScope?.cancel()
        assetScope = null
        holders.forEach { it.release() }
        super.onDetachedFromWindow()
    }
}
