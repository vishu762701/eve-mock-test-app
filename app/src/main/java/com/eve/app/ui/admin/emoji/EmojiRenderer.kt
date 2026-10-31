package com.eve.app.ui.admin.emoji

import coil.imageLoader
import coil.request.ImageRequest
import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.LruCache
import android.widget.TextView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
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

object EmojiRenderer {

    private const val MEMORY_CACHE_SIZE = 50
    private const val MAX_CONCURRENT_ANIMATIONS = 8

    private val compositionCache = LruCache<String, LottieComposition>(MEMORY_CACHE_SIZE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val activeAnimators = Collections.newSetFromMap(ConcurrentHashMap<LottieDrawable, Boolean>())
    private var categoryEmojiMap: Map<String, EmojiPlacement> = emptyMap()

    fun setCategoryEmojiMap(map: Map<String, EmojiPlacement>) {
        categoryEmojiMap = map
    }

    fun getCategoryEmoji(category: String): EmojiPlacement? {
        return categoryEmojiMap[category]
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

    fun attachEmoji(
        textView: TextView,
        baseText: CharSequence,
        emoji: EmojiPlacement?
    ) {
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
            val boundText = textView.text
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(emoji.url).target { drawable ->
                if (textView.text === boundText) {
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
                        val drawable = createLottieDrawable(textView, composition, isReducedMotion(context))
                        val finalSpan = AnimatedEmojiSpan(textView, emoji, lottieDrawable = drawable)
                        val ssb = SpannableStringBuilder(baseText).append(" ").append(placeholder)
                        ssb.setSpan(finalSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        textView.text = ssb
                    }
                return
            }
        }

        // Asynchronously load from disk cache or network
        scope.launch {
            val composition = loadCompositionAsync(context, emoji.url)
            if (composition != null) {
                compositionCache.put(emoji.url, composition)
                val drawable = createLottieDrawable(textView, composition, isReducedMotion(context))
                val finalSpan = AnimatedEmojiSpan(textView, emoji, lottieDrawable = drawable)
                val ssb = SpannableStringBuilder(baseText).append(" ").append(placeholder)
                ssb.setSpan(finalSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                textView.text = ssb
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
            val diskFile = getDiskCacheFile(context, url)
            if (diskFile.exists() && diskFile.length() > 0) {
                try {
                    val task = LottieCompositionFactory.fromJsonInputStreamSync(diskFile.inputStream(), url)
                    if (task.value != null) return@withContext task.value
                } catch (_: Exception) {}
            }

            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty() && bytes.size <= 300 * 1024) {
                        try {
                            FileOutputStream(diskFile).use { it.write(bytes) }
                        } catch (_: Exception) {}
                        val task = LottieCompositionFactory.fromJsonInputStreamSync(bytes.inputStream(), url)
                        return@withContext task.value
                    }
                }
            } catch (_: Exception) {}

            null
        }
}
