package com.eve.app.ui.premium.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.core.content.ContextCompat
import com.eve.app.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLIconRenderer(
    private val context: Context,
    val style: Int = FRAGMENT_STYLE,
    val type: Int = Icon3D.TYPE_STAR
) : GLSurfaceView.Renderer {

    companion object {
        const val FRAGMENT_STYLE = 0
        const val DIALOG_STYLE = 1
        private const val Z_NEAR = 1f
        private const val Z_FAR = 200f

        fun loadShader(type: Int, shaderSrc: String): Int {
            val shader = GLES20.glCreateShader(type)
            if (shader == 0) return 0

            GLES20.glShaderSource(shader, shaderSrc)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Could not compile shader $type: $log\nSource: $shaderSrc")
            }
            return shader
        }
    }

    private var mWidth = 0
    private var mHeight = 0
    var model: Icon3D? = null
    var angleX = 0f
    var angleX2 = 0f
    var angleX3 = 0f
    var angleY = 0f
    var white = 0f
    var golden = 0f

    private val mMVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mRotationMatrix = FloatArray(16)

    private var backgroundBitmap: Bitmap? = null

    var gradientStartX = 0f
    var gradientStartY = 0f
    var gradientScaleX = 0f
    var gradientScaleY = 0f

    var forceNight = false
    var night = false
    var color1 = 0xFFFFFFFF.toInt()
    var color2 = 0xFFE3ECFA.toInt()
    var isDarkBackground = false

    private var dt = 0f

    init {
        updateColors(false)
    }

    fun setDeltaTime(dt: Float) {
        this.dt = dt
    }

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        model?.destroy()
        model = Icon3D(context, type)
        backgroundBitmap?.let {
            model?.setBackground(it)
        }
        if (isDarkBackground) {
            model?.spec1 = 1f
            model?.spec2 = 0.2f
        }
    }

    override fun onSurfaceChanged(glUnused: GL10?, width: Int, height: Int) {
        mWidth = width
        mHeight = height
        GLES20.glViewport(0, 0, width, height)
        val aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
        val fov = 53.13f
        Matrix.perspectiveM(mProjectionMatrix, 0, fov, aspect, Z_NEAR, Z_FAR)
    }

    override fun onDrawFrame(glUnused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 100f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(mRotationMatrix, 0)

        Matrix.translateM(mRotationMatrix, 0, 0f, angleX2, 0f)
        Matrix.rotateM(mRotationMatrix, 0, -angleY, 1f, 0f, 0f)
        Matrix.rotateM(mRotationMatrix, 0, -angleX - angleX3, 0f, 1f, 0f)

        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mRotationMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        model?.let { m ->
            m.night = night
            m.gradientColor1 = color1
            m.gradientColor2 = color2
            m.draw(
                mMVPMatrix,
                mRotationMatrix,
                mWidth,
                mHeight,
                gradientStartX,
                gradientScaleX,
                gradientStartY,
                gradientScaleY,
                white,
                golden,
                dt
            )
        }
    }

    fun setBackground(gradientTextureBitmap: Bitmap) {
        model?.setBackground(gradientTextureBitmap)
        backgroundBitmap = gradientTextureBitmap
    }

    fun updateColors(isDark: Boolean) {
        night = forceNight || isDark
        color1 = ContextCompat.getColor(context, R.color.eve_premium_star_1)
        color2 = ContextCompat.getColor(context, R.color.eve_premium_star_2)
        isDarkBackground = isDark
    }
}
