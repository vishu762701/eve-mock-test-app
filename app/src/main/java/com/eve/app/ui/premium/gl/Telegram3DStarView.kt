package com.eve.app.ui.premium.gl

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.opengl.GLUtils
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.ThemeManager
import java.util.Random
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

class Telegram3DStarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    companion object {
        private const val EGL_OPENGL_ES2_BIT = 4
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098

        // Exact Telegram interpolators
        val EASE_OUT_QUINT = PathInterpolator(0.23f, 1f, 0.32f, 1f)
        val DEFAULT_INTERPOLATOR = PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)
        val EASE_OUT = PathInterpolator(0.0f, 0.0f, 0.58f, 1.0f)

        const val PREF_FORCE_STAR_FALLBACK = "debug_force_star_fallback"

        @Volatile
        var forceFallback: Boolean = false
    }

    var touched = false
    var lazyMode = true
    var mRenderer: GLIconRenderer? = null
    var fallbackView: View? = null
        set(value) {
            field = value
            if (glFailed && value != null) {
                value.visibility = VISIBLE
                visibility = GONE
            }
        }

    val isFailed: Boolean
        get() = glFailed

    private var mSurface: SurfaceTexture? = null
    private var mEglDisplay: EGLDisplay? = null
    private var mEglSurface: EGLSurface? = null
    private var mEglContext: EGLContext? = null
    private var mEgl: EGL10? = null
    private var eglConfig: EGLConfig? = null
    private var mGl: GL10? = null

    private var targetFrameDurationMillis = 16
    private var surfaceHeight = 0
    private var surfaceWidth = 0

    @Volatile
    var isRunning = false

    @Volatile
    private var paused = true
    private var rendererChanged = false
    private var glFailed = false
    @Volatile
    private var surfaceDimensionsChanged = false

    private var thread: RenderThread? = null
    private val idleDelay = 2000L

    private val animationsCount = 5
    private var animationPointer = 0
    private val animationIndexes = ArrayList<Int>()
    private var attached = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val random = Random()

    private var backAnimation: ValueAnimator? = null
    private var animatorSet: AnimatorSet? = null

    private val gestureDetector: GestureDetector

    init {
        isOpaque = false
        val prefs = context.getSharedPreferences("eve_prefs", Context.MODE_PRIVATE)
        val prefForced = com.eve.app.BuildConfig.DEBUG && prefs.getBoolean(PREF_FORCE_STAR_FALLBACK, false)
        if (forceFallback || prefForced) {
            triggerFallback()
        } else {
            val isDark = ThemeManager.isDarkMode(context)
            val renderer = GLIconRenderer(context, GLIconRenderer.FRAGMENT_STYLE, Icon3D.TYPE_STAR)
            renderer.updateColors(isDark)
            setRenderer(renderer)
            surfaceTextureListener = this
        }

        for (i in 0 until animationsCount) {
            animationIndexes.add(i)
        }
        animationIndexes.shuffle()

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                if (isAnimationDisabled()) return false
                cancelAnimations()
                mainHandler.removeCallbacks(idleAnimationRunnable)
                touched = true
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (isAnimationDisabled()) return false
                val rad = measuredWidth / 2f
                if (rad <= 0f) return true
                val toAngleX = (40f + random.nextInt(30)) * (rad - e.x) / rad
                val toAngleY = (40f + random.nextInt(30)) * (rad - e.y) / rad

                mainHandler.postDelayed({
                    cancelAnimations()
                    val r = mRenderer ?: return@postDelayed
                    if (Math.abs(r.angleX) > 10f) {
                        startBackAnimation()
                        return@postDelayed
                    }
                    mainHandler.removeCallbacks(idleAnimationRunnable)
                    val set = AnimatorSet()
                    val inTime = 220L

                    val v1 = ValueAnimator.ofFloat(r.angleX, toAngleX).apply {
                        addUpdateListener { r.angleX = it.animatedValue as Float }
                        duration = inTime
                        interpolator = EASE_OUT_QUINT
                    }

                    val v2 = ValueAnimator.ofFloat(toAngleX, 0f).apply {
                        addUpdateListener { r.angleX = it.animatedValue as Float }
                        startDelay = inTime
                        duration = 600L
                        interpolator = OvershootInterpolator(1.02f)
                    }

                    val v3 = ValueAnimator.ofFloat(r.angleY, toAngleY).apply {
                        addUpdateListener { r.angleY = it.animatedValue as Float }
                        duration = inTime
                        interpolator = EASE_OUT_QUINT
                    }

                    val v4 = ValueAnimator.ofFloat(toAngleY, 0f).apply {
                        addUpdateListener { r.angleY = it.animatedValue as Float }
                        startDelay = inTime
                        duration = 600L
                        interpolator = OvershootInterpolator(1.02f)
                    }

                    set.playTogether(v1, v2, v3, v4)
                    set.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            r.angleX = 0f
                            animatorSet = null
                            scheduleIdleAnimation(idleDelay)
                        }
                    })
                    animatorSet = set
                    set.start()
                }, 16)

                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (isAnimationDisabled()) return false
                mRenderer?.let {
                    it.angleX += distanceX * 0.5f
                    it.angleY += distanceY * 0.05f
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (isAnimationDisabled()) return false
                val r = mRenderer ?: return false
                val flingAngleX = (velocityX / 20f).coerceIn(-180f, 180f)
                r.angleX += flingAngleX
                startBackAnimation()
                return true
            }
        })
    }

    private fun isAnimationDisabled(): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun setRenderer(renderer: GLIconRenderer) {
        mRenderer = renderer
        rendererChanged = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            updateGradientBackground(w, h)
        }
    }

    fun updateTheme() {
        if (glFailed) return
        val isDark = ThemeManager.isDarkMode(context)
        mRenderer?.updateColors(isDark)
        if (measuredWidth > 0 && measuredHeight > 0) {
            updateGradientBackground(measuredWidth, measuredHeight)
        }
    }

    /**
     * Exact reproduction of Telegram's PremiumPreviewFragment.updateBackgroundImage()
     * and PremiumGradient.PremiumGradientTools.gradientMatrix(0, 0, W, H, 0, 0)
     */
    private fun updateGradientBackground(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        try {
            val c1 = ContextCompat.getColor(context, R.color.eve_premium_gradient_1)
            val c2 = ContextCompat.getColor(context, R.color.eve_premium_gradient_2)
            val c3 = ContextCompat.getColor(context, R.color.eve_premium_gradient_3)
            val c4 = ContextCompat.getColor(context, R.color.eve_premium_gradient_4)

            val size = 100
            val sizeHalf = 50

            val shader = LinearGradient(
                0f * size, 1f * size, 1.5f * size, 0f * size,
                intArrayOf(c1, c2, c3, c4),
                floatArrayOf(0f, 0.5f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )

            val gradientHeight = h + h
            val sx = w / size.toFloat()
            val sy = gradientHeight / size.toFloat()

            val matrix = Matrix()
            matrix.postScale(sx, sy, 75f, sizeHalf.toFloat())
            matrix.postTranslate(0f, (-gradientHeight).toFloat())
            shader.setLocalMatrix(matrix)

            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
            }

            canvas.save()
            canvas.scale(100f / w, 100f / h)
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            canvas.restore()

            mRenderer?.setBackground(bitmap)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        mSurface = surface
        setDimensions(width, height)
        val prefs = context.getSharedPreferences("eve_prefs", Context.MODE_PRIVATE)
        val prefForced = com.eve.app.BuildConfig.DEBUG && prefs.getBoolean(PREF_FORCE_STAR_FALLBACK, false)
        if (glFailed || forceFallback || prefForced) {
            triggerFallback()
            return
        }
        if (!lazyMode && !isPaused()) {
            startThread(surface, width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        setDimensions(width, height)
        surfaceDimensionsChanged = true
        if (width > 0 && height > 0) {
            updateGradientBackground(width, height)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopThread()
        mSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    fun startThread(surface: SurfaceTexture, width: Int, height: Int) {
        val prefs = context.getSharedPreferences("eve_prefs", Context.MODE_PRIVATE)
        val prefForced = com.eve.app.BuildConfig.DEBUG && prefs.getBoolean(PREF_FORCE_STAR_FALLBACK, false)
        if (glFailed || forceFallback || prefForced) {
            triggerFallback()
            return
        }
        stopThread()
        thread = RenderThread()
        mSurface = surface
        setDimensions(width, height)
        val refreshRate = 60
        targetFrameDurationMillis = Math.max(0, ((1f / refreshRate) * 1000).toInt() - 1)
        thread?.start()
    }

    fun stopThread() {
        val t = thread
        isRunning = false
        thread = null
        if (t != null && t != Thread.currentThread()) {
            try {
                t.interrupt()
                t.join(300)
            } catch (ignored: Exception) {
            }
        }
    }

    fun setDimensions(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    fun setPaused(isPaused: Boolean) {
        paused = isPaused
        if (isPaused) {
            mainHandler.removeCallbacks(idleAnimationRunnable)
            cancelAnimations()
            if (lazyMode) {
                stopThread()
            }
        } else {
            if (lazyMode && thread == null && mSurface != null && surfaceWidth > 0 && surfaceHeight > 0) {
                startThread(mSurface!!, surfaceWidth, surfaceHeight)
            }
            if (!isAnimationDisabled() && !glFailed) {
                scheduleIdleAnimation(idleDelay)
            }
        }
    }

    fun isPaused(): Boolean = paused

    private fun shouldSleep(): Boolean = isPaused() || mRenderer == null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (glFailed || isAnimationDisabled()) {
            return super.onTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                touched = false
                startBackAnimation()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    fun startBackAnimation() {
        if (glFailed) return
        if (isAnimationDisabled()) {
            mRenderer?.let {
                it.angleX = 0f
                it.angleY = 0f
                it.angleX2 = 0f
            }
            return
        }
        cancelAnimations()
        val r = mRenderer ?: return
        val fromX = r.angleX
        val fromY = r.angleY
        val fromX2 = r.angleX2

        val anim = ValueAnimator.ofFloat(1f, 0f)
        anim.addUpdateListener { valueAnimator ->
            val v = valueAnimator.animatedValue as Float
            r.angleX = v * fromX
            r.angleX2 = v * fromX2
            r.angleY = v * fromY
        }
        anim.duration = 600L
        anim.interpolator = OvershootInterpolator(1.02f)
        anim.start()
        backAnimation = anim
        scheduleIdleAnimation(idleDelay)
    }

    fun startEnterAnimation(angle: Float = -180f, delay: Long = 0L) {
        if (glFailed) return
        val r = mRenderer ?: return
        if (isAnimationDisabled()) {
            r.angleX = 0f
            r.angleY = 0f
            return
        }
        r.angleX = angle
        mainHandler.postDelayed({
            startBackAnimation()
        }, delay)
    }

    fun cancelAnimations() {
        backAnimation?.removeAllListeners()
        backAnimation?.cancel()
        backAnimation = null

        animatorSet?.removeAllListeners()
        animatorSet?.cancel()
        animatorSet = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        rendererChanged = true
        if (!isAnimationDisabled() && !glFailed) {
            scheduleIdleAnimation(idleDelay)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopThread()
        cancelAnimations()
        mainHandler.removeCallbacks(idleAnimationRunnable)
        mRenderer?.let {
            it.angleX = 0f
            it.angleY = 0f
            it.angleX2 = 0f
        }
        attached = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        setPaused(visibility != VISIBLE)
    }

    private val idleAnimationRunnable = Runnable {
        if ((animatorSet != null && animatorSet?.isRunning == true) ||
            (backAnimation != null && backAnimation?.isRunning == true)
        ) {
            scheduleIdleAnimation(idleDelay)
        } else {
            startIdleAnimation()
        }
    }

    fun scheduleIdleAnimation(time: Long) {
        if (glFailed || isAnimationDisabled()) return
        mainHandler.removeCallbacks(idleAnimationRunnable)
        mainHandler.postDelayed(idleAnimationRunnable, time)
    }

    private fun startIdleAnimation() {
        if (glFailed || !attached || !isShown || mRenderer == null || isAnimationDisabled()) return

        if (animationPointer >= animationIndexes.size) {
            animationIndexes.shuffle()
            animationPointer = 0
        }
        val i = animationIndexes[animationPointer]
        animationPointer++

        when (i) {
            0 -> pullAnimation()
            1 -> slowFlipAnimation()
            2 -> sleepAnimation()
            else -> flipAnimation()
        }
    }

    private fun slowFlipAnimation() {
        val r = mRenderer ?: return
        val set = AnimatorSet()
        val v1 = ValueAnimator.ofFloat(r.angleX, 360f).apply {
            addUpdateListener { r.angleX = it.animatedValue as Float }
            duration = 8000L
            interpolator = DEFAULT_INTERPOLATOR
        }
        set.playTogether(v1)
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                r.angleX = 0f
                animatorSet = null
                scheduleIdleAnimation(idleDelay)
            }
        })
        animatorSet = set
        set.start()
    }

    private fun pullAnimation() {
        val r = mRenderer ?: return
        val i = Math.abs(random.nextInt() % 4)
        val set = AnimatorSet()

        if (i == 0) {
            val a = 48f
            val v1 = ValueAnimator.ofFloat(r.angleY, a).apply {
                addUpdateListener { r.angleY = it.animatedValue as Float }
                duration = 2300L
                interpolator = EASE_OUT_QUINT
            }
            val v2 = ValueAnimator.ofFloat(a, 0f).apply {
                addUpdateListener { r.angleY = it.animatedValue as Float }
                duration = 500L
                startDelay = 2300L
                interpolator = OvershootInterpolator(1.02f)
            }
            set.playTogether(v1, v2)
        } else {
            val dg = if (i == 2) -485f else 485f
            val v1 = ValueAnimator.ofFloat(r.angleX, dg).apply {
                addUpdateListener { r.angleX = it.animatedValue as Float }
                duration = 3000L
                interpolator = EASE_OUT_QUINT
            }
            val v2 = ValueAnimator.ofFloat(dg, 0f).apply {
                addUpdateListener { r.angleX = it.animatedValue as Float }
                duration = 1000L
                startDelay = 3000L
                interpolator = OvershootInterpolator(1.02f)
            }
            set.playTogether(v1, v2)
        }

        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                r.angleX = 0f
                animatorSet = null
                scheduleIdleAnimation(idleDelay)
            }
        })
        animatorSet = set
        set.start()
    }

    private fun flipAnimation() {
        val r = mRenderer ?: return
        val set = AnimatorSet()
        val v1 = ValueAnimator.ofFloat(r.angleX, 180f).apply {
            addUpdateListener { r.angleX = it.animatedValue as Float }
            duration = 600L
            interpolator = DEFAULT_INTERPOLATOR
        }
        val v2 = ValueAnimator.ofFloat(180f, 360f).apply {
            addUpdateListener { r.angleX = it.animatedValue as Float }
            duration = 600L
            startDelay = 2000L
            interpolator = DEFAULT_INTERPOLATOR
        }
        set.playTogether(v1, v2)
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                r.angleX = 0f
                animatorSet = null
                scheduleIdleAnimation(idleDelay)
            }
        })
        animatorSet = set
        set.start()
    }

    private fun sleepAnimation() {
        val r = mRenderer ?: return
        val set = AnimatorSet()
        val v1 = ValueAnimator.ofFloat(r.angleX, 184f).apply {
            addUpdateListener { r.angleX = it.animatedValue as Float }
            duration = 600L
            interpolator = EASE_OUT
        }
        val v2 = ValueAnimator.ofFloat(r.angleY, 50f).apply {
            addUpdateListener { r.angleY = it.animatedValue as Float }
            duration = 600L
            interpolator = EASE_OUT
        }
        val v3 = ValueAnimator.ofFloat(180f, 0f).apply {
            addUpdateListener { r.angleX = it.animatedValue as Float }
            duration = 800L
            startDelay = 10000L
            interpolator = OvershootInterpolator(1.02f)
        }
        val v4 = ValueAnimator.ofFloat(60f, 0f).apply {
            addUpdateListener { r.angleY = it.animatedValue as Float }
            duration = 800L
            startDelay = 10000L
            interpolator = OvershootInterpolator(1.02f)
        }
        val v5 = ValueAnimator.ofFloat(0f, 2f, -3f, 2f, -1f, 2f, -3f, 2f, -1f, 0f).apply {
            addUpdateListener { r.angleX2 = it.animatedValue as Float }
            duration = 10000L
            interpolator = LinearInterpolator()
        }

        set.playTogether(v1, v2, v3, v4, v5)
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                r.angleX = 0f
                animatorSet = null
                scheduleIdleAnimation(idleDelay)
            }
        })
        animatorSet = set
        set.start()
    }

    private fun triggerFallback() {
        glFailed = true
        isRunning = false
        mainHandler.post {
            cancelAnimations()
            mainHandler.removeCallbacks(idleAnimationRunnable)
            visibility = GONE
            fallbackView?.visibility = VISIBLE
        }
    }

    private inner class RenderThread : Thread("Telegram3DStarRenderThread") {
        override fun run() {
            isRunning = true

            try {
                initGL()
            } catch (e: Throwable) {
                e.printStackTrace()
                triggerFallback()
                return
            }

            var lastFrameTime = System.currentTimeMillis()

            try {
                while (isRunning) {
                    while (mRenderer == null && isRunning) {
                        try {
                            sleep(100)
                        } catch (e: InterruptedException) {
                        }
                    }

                    if (!isRunning) break

                    if (rendererChanged) {
                        try {
                            initializeRenderer(mRenderer)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            triggerFallback()
                            break
                        }
                        rendererChanged = false
                    }

                    if (surfaceDimensionsChanged) {
                        surfaceDimensionsChanged = false
                        try {
                            mRenderer?.onSurfaceChanged(mGl, surfaceWidth, surfaceHeight)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            triggerFallback()
                            break
                        }
                    }

                    try {
                        if (!shouldSleep()) {
                            val now = System.currentTimeMillis()
                            val dt = (now - lastFrameTime) / 1000f
                            lastFrameTime = now
                            drawSingleFrame(dt)
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        triggerFallback()
                        break
                    }

                    try {
                        if (shouldSleep()) {
                            sleep(100)
                        } else {
                            val thisFrameTime = System.currentTimeMillis()
                            val timeDiff = thisFrameTime - lastFrameTime
                            val sleepTime = targetFrameDurationMillis - timeDiff
                            if (sleepTime > 0) {
                                sleep(sleepTime)
                            }
                        }
                    } catch (ignore: InterruptedException) {
                    }
                }
            } finally {
                destroyGL()
            }
        }
    }

    @Synchronized
    private fun initializeRenderer(renderer: GLIconRenderer?) {
        if (renderer != null && isRunning) {
            renderer.onSurfaceCreated(mGl, eglConfig)
            renderer.onSurfaceChanged(mGl, surfaceWidth, surfaceHeight)
        }
    }

    @Synchronized
    private fun drawSingleFrame(dt: Float) {
        checkCurrent()
        mRenderer?.let {
            it.setDeltaTime(dt)
            it.onDrawFrame(mGl)
        }
        mEgl?.eglSwapBuffers(mEglDisplay, mEglSurface)
    }

    private fun checkCurrent() {
        val curCtx = mEgl?.eglGetCurrentContext()
        val curSurf = mEgl?.eglGetCurrentSurface(EGL10.EGL_DRAW)
        if (mEglContext != curCtx || mEglSurface != curSurf) {
            if (mEgl?.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext) == false) {
                throw RuntimeException("eglMakeCurrent failed")
            }
        }
    }

    private fun initGL() {
        val surfTexture = mSurface ?: throw RuntimeException("mSurface is null")
        val egl = EGLContext.getEGL() as EGL10
        mEgl = egl

        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (display == null || display == EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }
        mEglDisplay = display

        val version = IntArray(2)
        if (!egl.eglInitialize(display, version)) {
            throw RuntimeException("eglInitialize failed")
        }

        val configSpec = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_NONE
        )

        val configsCount = IntArray(1)
        val configs = arrayOfNulls<EGLConfig>(1)
        if (!egl.eglChooseConfig(display, configSpec, configs, 1, configsCount) || configsCount[0] <= 0) {
            throw RuntimeException("eglChooseConfig failed")
        }
        val cfg = configs[0] ?: throw RuntimeException("eglConfig null")
        eglConfig = cfg

        val attribList = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
        val ctx = egl.eglCreateContext(display, cfg, EGL10.EGL_NO_CONTEXT, attribList)
        if (ctx == null || ctx == EGL10.EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext failed")
        }
        mEglContext = ctx

        val surf = egl.eglCreateWindowSurface(display, cfg, surfTexture, null)
        if (surf == null || surf == EGL10.EGL_NO_SURFACE) {
            throw RuntimeException("eglCreateWindowSurface failed")
        }
        mEglSurface = surf

        if (!egl.eglMakeCurrent(display, surf, surf, ctx)) {
            throw RuntimeException("eglMakeCurrent failed")
        }

        mGl = ctx.gl as? GL10 ?: throw RuntimeException("ctx.gl null")
    }

    private fun destroyGL() {
        try {
            mRenderer?.model?.destroy()
            mEgl?.let { egl ->
                mEglDisplay?.let { dpy ->
                    if (dpy != EGL10.EGL_NO_DISPLAY) {
                        egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                        mEglSurface?.let { surf ->
                            if (surf != EGL10.EGL_NO_SURFACE) {
                                egl.eglDestroySurface(dpy, surf)
                            }
                        }
                        mEglContext?.let { ctx ->
                            if (ctx != EGL10.EGL_NO_CONTEXT) {
                                egl.eglDestroyContext(dpy, ctx)
                            }
                        }
                        // Do not terminate EGL display connection on Android;
                        // it destroys process-wide display connection and crashes Android HWUI rendering.
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            mEglSurface = null
            mEglContext = null
            mEglDisplay = null
            mEgl = null
            mGl = null
        }
    }
}
