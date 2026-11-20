package com.eve.app.ui.admin.emoji

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.eve.app.data.model.EmojiItem
import com.eve.app.data.model.EmojiPlacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * End-to-end Motion Emoji Renderer & Recent Store.
 * Mirrors Telegram's AnimatedEmojiDrawable lifecycle: bounded concurrent playback,
 * automatic animator release on completion/cancel, recycle-safe TextView binding,
 * reduced-motion respect, live looping preview binding, and persisted Recent emojis.
 */
object EmojiRenderer {

    private const val MEMORY_CACHE_SIZE = 64
    private const val MAX_CONCURRENT_ANIMATIONS = 8
    private const val PREFS_RECENT_EMOJI = "eve_recent_motion_emojis_v1"
    private const val KEY_RECENT_IDS = "recent_ids"
    private const val MAX_RECENT_COUNT = 24

    private val compositionCache = LruCache<String, LottieComposition>(MEMORY_CACHE_SIZE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bindTokens = ConcurrentHashMap<Int, Long>()
    private val tokenGenerator = AtomicLong(0L)

    private val activeAnimators = Collections.newSetFromMap(ConcurrentHashMap<LottieDrawable, Boolean>())
    private var categoryEmojiMap: Map<String, EmojiPlacement> = emptyMap()

    fun setCategoryEmojiMap(map: Map<String, EmojiPlacement>) {
        categoryEmojiMap = map
    }

    fun getCategoryEmoji(category: String): EmojiPlacement? {
        return categoryEmojiMap[category]
    }

    fun recordRecent(context: Context, itemId: String) {
        if (itemId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_RECENT_EMOJI, Context.MODE_PRIVATE)
        val current = getRecentIds(context).toMutableList()
        current.remove(itemId)
        current.add(0, itemId)
        val trimmed = current.take(MAX_RECENT_COUNT)
        prefs.edit().putString(KEY_RECENT_IDS, trimmed.joinToString(",")).apply()
    }

    fun getRecentIds(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_RECENT_EMOJI, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT_IDS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun pauseAll() {
        for (drawable in activeAnimators) {
            if (drawable.isAnimating) {
                drawable.pauseAnimation()
            }
        }
    }

    fun resumeAll() {
        for (drawable in activeAnimators) {
            if (!drawable.isAnimating) {
                drawable.resumeAnimation()
            }
        }
    }

    private fun isReducedMotion(context: Context): Boolean {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    private fun getDiskCacheFile(context: Context, url: String): File {
        val hash = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val dir = File(context.cacheDir, "emoji_cache").apply { if (!exists()) mkdirs() }
        return File(dir, "$hash.json")
    }

    /**
     * Binds a motion or static emoji into a preview/grid triplet (LottieAnimationView + ImageView + fallback TextView)
     * with recycle cancellation, failure fallback to Unicode alt emoji, and reduced-motion support.
     */
    fun bindPreview(
        lottieView: LottieAnimationView,
        staticView: ImageView?,
        altView: TextView?,
        item: EmojiItem,
        loop: Boolean = true
    ) {
        lottieView.cancelAnimation()
        lottieView.clearAnimation()
        lottieView.visibility = View.GONE
        staticView?.visibility = View.GONE
        altView?.text = item.altEmoji.ifBlank { "✨" }
        altView?.visibility = View.VISIBLE

        val reducedMotion = isReducedMotion(lottieView.context)
        val repeatCount = if (loop && !reducedMotion) LottieDrawable.INFINITE else 0

        if (item.kind == "static" && item.url.isNotBlank() && staticView != null) {
            staticView.visibility = View.VISIBLE
            staticView.load(item.url) {
                listener(
                    onSuccess = { _, _ -> altView?.visibility = View.GONE },
                    onError = { _, _ ->
                        staticView.visibility = View.GONE
                        altView?.visibility = View.VISIBLE
                    }
                )
            }
            return
        }

        if (item.url.startsWith("res:raw/")) {
            val rawName = item.url.removePrefix("res:raw/")
            val resId = lottieView.context.resources.getIdentifier(rawName, "raw", lottieView.context.packageName)
            if (resId != 0) {
                lottieView.visibility = View.VISIBLE
                altView?.visibility = View.GONE
                lottieView.setFailureListener {
                    lottieView.visibility = View.GONE
                    altView?.visibility = View.VISIBLE
                }
                lottieView.repeatCount = repeatCount
                lottieView.setAnimation(resId)
                if (reducedMotion) lottieView.frame = 0 else lottieView.playAnimation()
            }
            return
        }

        if (item.url.isNotBlank()) {
            lottieView.visibility = View.VISIBLE
            altView?.visibility = View.GONE
            lottieView.setFailureListener {
                lottieView.visibility = View.GONE
                altView?.visibility = View.VISIBLE
            }
            lottieView.repeatCount = repeatCount
            lottieView.setAnimationFromUrl(item.url)
            if (reducedMotion) lottieView.frame = 0 else lottieView.playAnimation()
        }
    }

    fun attachEmoji(
        textView: TextView,
        baseText: CharSequence,
        emoji: EmojiPlacement?
    ) {
        val viewKey = System.identityHashCode(textView)
        val token = tokenGenerator.incrementAndGet()
        bindTokens[viewKey] = token

        if (emoji == null || emoji.url.isBlank()) {
            textView.text = baseText
            return
        }

        val context = textView.context
        val builder = SpannableStringBuilder(baseText)
        builder.append(" ")
        val spanStart = builder.length
        val placeholder = emoji.alt.ifBlank { " " }
        builder.append(placeholder)
        val spanEnd = builder.length

        if (emoji.kind == "static") {
            textView.text = builder
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(emoji.url).target { drawable ->
                if (bindTokens[viewKey] == token) {
                    val span = AnimatedEmojiSpan(textView, emoji, staticDrawable = drawable)
                    val result = SpannableStringBuilder(builder)
                    result.setSpan(span, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    textView.text = result
                }
            }.build())
            return
        }

        // Check if cached in memory
        val cached = compositionCache.get(emoji.url)
        if (cached != null) {
            val drawable = createLottieDrawable(textView, cached, isReducedMotion(context))
            val span = AnimatedEmojiSpan(textView, emoji, lottieDrawable = drawable)
            builder.setSpan(span, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textView.text = builder
            return
        }

        // Initially render with plain alt emoji fallback while async loading
        val fallbackSpan = AnimatedEmojiSpan(textView, emoji)
        builder.setSpan(fallbackSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = builder

        // Handle bundled raw resource
        if (emoji.url.startsWith("res:raw/")) {
            val rawName = emoji.url.removePrefix("res:raw/")
            val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
            if (resId != 0) {
                LottieCompositionFactory.fromRawRes(context, resId)
                    .addListener { composition ->
                        compositionCache.put(emoji.url, composition)
                        if (bindTokens[viewKey] == token) {
                            val drawable = createLottieDrawable(textView, composition, isReducedMotion(context))
                            val finalSpan = AnimatedEmojiSpan(textView, emoji, lottieDrawable = drawable)
                            val ssb = SpannableStringBuilder(baseText).append(" ").append(placeholder)
                            ssb.setSpan(finalSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            textView.text = ssb
                        }
                    }
                return
            }
        }

        // Asynchronously load from disk cache or network
        scope.launch {
            val composition = loadCompositionAsync(context, emoji.url)
            if (composition != null) {
                compositionCache.put(emoji.url, composition)
                if (bindTokens[viewKey] == token) {
                    val drawable = createLottieDrawable(textView, composition, isReducedMotion(context))
                    val finalSpan = AnimatedEmojiSpan(textView, emoji, lottieDrawable = drawable)
                    val ssb = SpannableStringBuilder(baseText).append(" ").append(placeholder)
                    ssb.setSpan(finalSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    textView.text = ssb
                }
            }
        }
    }

    private fun createLottieDrawable(
        textView: TextView,
        composition: LottieComposition,
        reducedMotion: Boolean
    ): LottieDrawable {
        val drawable = LottieDrawable()
        drawable.composition = composition
        drawable.repeatCount = 0 // Play once per appearance, matching owner rule

        drawable.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                textView.invalidate()
            }
            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                textView.postDelayed(what, `when`)
            }
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                textView.removeCallbacks(what)
            }
        }

        drawable.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                activeAnimators.remove(drawable)
            }

            override fun onAnimationCancel(animation: Animator) {
                activeAnimators.remove(drawable)
            }
        })

        if (reducedMotion) {
            drawable.frame = 0
        } else if (activeAnimators.size < MAX_CONCURRENT_ANIMATIONS) {
            activeAnimators.add(drawable)
            drawable.start()
        } else {
            drawable.frame = 0
        }

        return drawable
    }

    private suspend fun loadCompositionAsync(context: Context, url: String): LottieComposition? =
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = getDiskCacheFile(context, url)
                val jsonString = if (cacheFile.exists() && cacheFile.length() > 0) {
                    cacheFile.readText()
                } else {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.requestMethod = "GET"
                    if (connection.responseCode == 200) {
                        val bytes = connection.inputStream.use { it.readBytes() }
                        FileOutputStream(cacheFile).use { it.write(bytes) }
                        String(bytes, Charsets.UTF_8)
                    } else {
                        null
                    }
                }

                if (jsonString != null) {
                    val result = LottieCompositionFactory.fromJsonStringSync(jsonString, url)
                    result.value
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
}
