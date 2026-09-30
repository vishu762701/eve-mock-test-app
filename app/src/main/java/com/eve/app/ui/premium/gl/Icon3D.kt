package com.eve.app.ui.premium.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import com.eve.app.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern

class Icon3D(context: Context, val type: Int = TYPE_STAR) {

    companion object {
        const val TYPE_STAR = 0
        private val starModel = arrayOf("models/star.binobj")

        @Volatile
        private var cachedStarTexture: Bitmap? = null

        @Synchronized
        fun getStarTextureBitmap(): Bitmap {
            cachedStarTexture?.let {
                if (!it.isRecycled) return it
            }
            val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            try {
                val path = PathParser.createPathFromPathData(
                    "M242.441216,1023.96673 C484.760949,868.195472 619.565351,780.153383 646.854423,759.840461 " +
                    "C738.267847,691.795866 863.983039,585.091695 1024,439.727947 L1024,363.976818 " +
                    "C810.351061,332.876819 639.684394,317.32682 512,317.32682 " +
                    "C384.315606,317.32682 213.648939,332.876819 0,363.976818 " +
                    "L0,1023.96673 L242.441216,1023.96673 Z"
                )
                val matrix = android.graphics.Matrix().apply {
                    setScale(240f / 1024f, 240f / 1024f)
                }
                path.transform(matrix)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawPath(path, paint)
            } catch (e: Throwable) {
                canvas.drawColor(Color.WHITE)
            }
            cachedStarTexture = bitmap
            return bitmap
        }
    }

    private var mProgramObject = 0
    private var mMVPMatrixHandle = 0
    private var mWorldMatrixHandle = 0
    private var mVertices: Array<FloatBuffer?>
    private var mTextures: Array<FloatBuffer?>
    private var mNormals: Array<FloatBuffer?>

    private var mTextureUniformHandle = 0
    private var mNormalMapUniformHandle = 0
    private var mBackgroundTextureUniformHandle = 0
    private var mBackgroundTextureHandle = 0
    private var mVerticesHandle = 0
    private var mTextureCoordinateHandle = 0
    private var mNormalCoordinateHandle = 0
    private var xOffsetHandle = 0
    private var alphaHandle = 0
    private var mTextureDataHandle = 0
    private var whiteHandle = 0
    private var goldenHandle = 0
    private var xOffset = 0f

    private var trianglesCount: IntArray
    private var enterAlpha = 0f

    var spec1 = 2f
    var spec2 = 0.13f
    var diffuse = 1f
    var gradientColor1 = 0xFFFFFFFF.toInt()
    var gradientColor2 = 0xFFE3ECFA.toInt()
    var normalSpec = 0.2f
    var normalSpecColor = Color.WHITE
    var specColor = Color.WHITE
    var night = false

    private var specHandleTop = 0
    private var specHandleBottom = 0
    private var diffuseHandle = 0
    private var gradientColor1Handle = 0
    private var gradientColor2Handle = 0
    private var normalSpecHandle = 0
    private var normalSpecColorHandle = 0
    private var specColorHandle = 0
    private var resolutionHandle = 0
    private var gradientPositionHandle = 0
    private var modelIndexHandle = 0
    private var modelIndex2Handle = 0
    private var behindHandle = 0
    private var typeHandle = 0
    private var nightHandle = 0
    private var timeHandle = 0

    private var texture: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    val n: Int
    private var buffers: IntArray? = null
    private var time = 0f

    init {
        val modelPaths = starModel
        val modelScale = 1.0f

        n = modelPaths.size
        mVertices = arrayOfNulls(n)
        mTextures = arrayOfNulls(n)
        mNormals = arrayOfNulls(n)
        trianglesCount = IntArray(n)

        for (i in 0 until n) {
            val obj = ObjLoader(context, modelPaths[i], modelScale)

            val vBuf = ByteBuffer.allocateDirect(obj.positions.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            vBuf.put(obj.positions).position(0)
            mVertices[i] = vBuf

            val tBuf = ByteBuffer.allocateDirect(obj.textureCoordinates.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            tBuf.put(obj.textureCoordinates).position(0)
            mTextures[i] = tBuf

            val nBuf = ByteBuffer.allocateDirect(obj.normals.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            nBuf.put(obj.normals).position(0)
            mNormals[i] = nBuf

            trianglesCount[i] = obj.positions.size
        }

        generateTexture(context)

        val fragmentShaderSource = "shaders/fragment4.glsl"
        val vertexShaderSource = "shaders/vertex2.glsl"

        val vertexShader = GLIconRenderer.loadShader(
            GLES20.GL_VERTEX_SHADER,
            preprocessShader(loadFromAsset(context, vertexShaderSource))
        )
        val fragmentShader = GLIconRenderer.loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            preprocessShader(loadFromAsset(context, fragmentShaderSource))
        )

        val programObject = GLES20.glCreateProgram()
        GLES20.glAttachShader(programObject, vertexShader)
        GLES20.glAttachShader(programObject, fragmentShader)
        GLES20.glLinkProgram(programObject)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0)
        mProgramObject = programObject

        initGl(context)
    }

    private fun initGl(context: Context) {
        GLES20.glUseProgram(mProgramObject)

        mVerticesHandle = GLES20.glGetAttribLocation(mProgramObject, "vPosition")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramObject, "a_TexCoordinate")
        mNormalCoordinateHandle = GLES20.glGetAttribLocation(mProgramObject, "a_Normal")

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramObject, "u_Texture")
        mNormalMapUniformHandle = GLES20.glGetUniformLocation(mProgramObject, "u_NormalMap")
        mBackgroundTextureUniformHandle = GLES20.glGetUniformLocation(mProgramObject, "u_BackgroundTexture")
        xOffsetHandle = GLES20.glGetUniformLocation(mProgramObject, "f_xOffset")
        alphaHandle = GLES20.glGetUniformLocation(mProgramObject, "f_alpha")
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix")
        mWorldMatrixHandle = GLES20.glGetUniformLocation(mProgramObject, "world")
        whiteHandle = GLES20.glGetUniformLocation(mProgramObject, "white")
        goldenHandle = GLES20.glGetUniformLocation(mProgramObject, "golden")

        specHandleTop = GLES20.glGetUniformLocation(mProgramObject, "spec1")
        specHandleBottom = GLES20.glGetUniformLocation(mProgramObject, "spec2")
        diffuseHandle = GLES20.glGetUniformLocation(mProgramObject, "u_diffuse")
        gradientColor1Handle = GLES20.glGetUniformLocation(mProgramObject, "gradientColor1")
        gradientColor2Handle = GLES20.glGetUniformLocation(mProgramObject, "gradientColor2")
        normalSpecColorHandle = GLES20.glGetUniformLocation(mProgramObject, "normalSpecColor")
        normalSpecHandle = GLES20.glGetUniformLocation(mProgramObject, "normalSpec")
        specColorHandle = GLES20.glGetUniformLocation(mProgramObject, "specColor")
        resolutionHandle = GLES20.glGetUniformLocation(mProgramObject, "resolution")
        gradientPositionHandle = GLES20.glGetUniformLocation(mProgramObject, "gradientPosition")
        modelIndexHandle = GLES20.glGetUniformLocation(mProgramObject, "modelIndex")
        modelIndex2Handle = GLES20.glGetUniformLocation(mProgramObject, "modelIndex2")
        behindHandle = GLES20.glGetUniformLocation(mProgramObject, "behind")
        typeHandle = GLES20.glGetUniformLocation(mProgramObject, "type")
        nightHandle = GLES20.glGetUniformLocation(mProgramObject, "night")
        timeHandle = GLES20.glGetUniformLocation(mProgramObject, "time")

        val buf = IntArray(3 * n)
        GLES20.glGenBuffers(3 * n, buf, 0)
        buffers = buf

        for (i in 0 until n) {
            val textures = mTextures[i]!!
            val normals = mNormals[i]!!
            val vertices = mVertices[i]!!

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[3 * i + 0])
            textures.position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * textures.capacity(), textures, GLES20.GL_STATIC_DRAW)
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
            textures.clear()

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[3 * i + 1])
            normals.position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * normals.capacity(), normals, GLES20.GL_STATIC_DRAW)
            GLES20.glEnableVertexAttribArray(mNormalCoordinateHandle)
            normals.clear()

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[3 * i + 2])
            vertices.position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * vertices.capacity(), vertices, GLES20.GL_STATIC_DRAW)
            GLES20.glEnableVertexAttribArray(mVerticesHandle)
            vertices.clear()
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        // Load flecks normal map from assets
        val flecksBitmap = getBitmapFromAsset(context, "flecks.png")
        val normalMap = IntArray(1)
        GLES20.glGenTextures(1, normalMap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normalMap[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        if (flecksBitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, flecksBitmap, 0)
            flecksBitmap.recycle()
        }

        // Background texture handle
        val bgHandle = IntArray(1)
        GLES20.glGenTextures(1, bgHandle, 0)
        mBackgroundTextureHandle = bgHandle[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBackgroundTextureHandle)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // Load star specular texture (exact SVG path rasterized to 240x240 white bitmap)
        val starBitmap = getStarTextureBitmap()
        val starTexHandle = IntArray(1)
        GLES20.glGenTextures(1, starTexHandle, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, starTexHandle[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, starBitmap, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, starTexHandle[0])
        GLES20.glUniform1i(mTextureUniformHandle, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normalMap[0])
        GLES20.glUniform1i(mNormalMapUniformHandle, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBackgroundTextureHandle)
        GLES20.glUniform1i(mBackgroundTextureUniformHandle, 2)
    }

    private fun generateTexture(context: Context) {
        val c1 = ContextCompat.getColor(context, R.color.eve_premium_gradient_1)
        val c2 = ContextCompat.getColor(context, R.color.eve_premium_gradient_2)
        val c3 = ContextCompat.getColor(context, R.color.eve_premium_gradient_3)
        val c4 = ContextCompat.getColor(context, R.color.eve_premium_gradient_4)

        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 100f, 150f, 0f,
            intArrayOf(c1, c2, c3, c4),
            floatArrayOf(0f, 0.5f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        texture = bmp

        val texHandle = IntArray(1)
        GLES20.glGenTextures(1, texHandle, 0)
        mTextureDataHandle = texHandle[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
    }

    fun draw(
        mvpMatrix: FloatArray,
        worldMatrix: FloatArray,
        width: Int,
        height: Int,
        gradientStartX: Float,
        gradientScaleX: Float,
        gradientStartY: Float,
        gradientScaleY: Float,
        white: Float,
        golden: Float,
        dt: Float
    ) {
        val bgBmp = backgroundBitmap
        if (bgBmp != null) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBackgroundTextureHandle)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bgBmp, 0)
            backgroundBitmap = null
        }

        GLES20.glUniform1i(mTextureUniformHandle, 0)
        GLES20.glUniform1f(xOffsetHandle, xOffset)
        GLES20.glUniform1f(alphaHandle, enterAlpha)
        GLES20.glUniform1f(whiteHandle, white)
        GLES20.glUniform1f(goldenHandle, golden)
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mWorldMatrixHandle, 1, false, worldMatrix, 0)

        GLES20.glUniform1f(specHandleTop, spec1)
        GLES20.glUniform1f(specHandleBottom, spec2)
        GLES20.glUniform1f(diffuseHandle, diffuse)
        GLES20.glUniform1f(normalSpecHandle, normalSpec)

        GLES20.glUniform3f(
            gradientColor1Handle,
            Color.red(gradientColor1) / 255f,
            Color.green(gradientColor1) / 255f,
            Color.blue(gradientColor1) / 255f
        )
        GLES20.glUniform3f(
            gradientColor2Handle,
            Color.red(gradientColor2) / 255f,
            Color.green(gradientColor2) / 255f,
            Color.blue(gradientColor2) / 255f
        )
        GLES20.glUniform3f(
            normalSpecColorHandle,
            Color.red(normalSpecColor) / 255f,
            Color.green(normalSpecColor) / 255f,
            Color.blue(normalSpecColor) / 255f
        )
        GLES20.glUniform3f(
            specColorHandle,
            Color.red(specColor) / 255f,
            Color.green(specColor) / 255f,
            Color.blue(specColor) / 255f
        )
        GLES20.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
        GLES20.glUniform4f(gradientPositionHandle, gradientStartX, gradientScaleX, gradientStartY, gradientScaleY)
        GLES20.glUniform1i(nightHandle, if (night) 1 else 0)

        time += dt
        GLES20.glUniform1f(timeHandle, time)

        for (i in 0 until n) {
            drawModel(i, false)
        }

        if (enterAlpha < 1f) {
            enterAlpha += 16f / 220f
            if (enterAlpha > 1f) enterAlpha = 1f
        }
        xOffset += 0.0005f
        if (xOffset > 1f) xOffset -= 1f
    }

    private fun drawModel(modelIndex: Int, behind: Boolean) {
        val i = modelIndex
        val buf = buffers ?: return
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[3 * i + 0])
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[3 * i + 1])
        GLES20.glVertexAttribPointer(mNormalCoordinateHandle, 3, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[3 * i + 2])
        GLES20.glVertexAttribPointer(mVerticesHandle, 3, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glUniform1i(modelIndexHandle, i)
        GLES20.glUniform1i(modelIndex2Handle, i)
        GLES20.glUniform1i(behindHandle, if (behind) 1 else 0)
        GLES20.glUniform1i(typeHandle, type)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, trianglesCount[i] / 3)
    }

    private fun preprocessShader(code: String): String {
        val pattern = Pattern.compile("RGB#([0-9a-fA-F]{6})")
        val matcher = pattern.matcher(code)
        val result = StringBuffer()
        while (matcher.find()) {
            val hex = matcher.group(1) ?: continue
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            val replacement = String.format(Locale.US, "vec3(%.3f, %.3f, %.3f)", r / 255.0, g / 255.0, b / 255.0)
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        return result.toString()
    }

    private fun loadFromAsset(context: Context, name: String): String {
        val sb = StringBuilder()
        try {
            val inputStream = context.assets.open(name)
            val br = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            var str: String?
            while (br.readLine().also { str = it } != null) {
                sb.append(str).append("\n")
            }
            br.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    private fun getBitmapFromAsset(context: Context, filePath: String): Bitmap? {
        return try {
            val istr = context.assets.open(filePath)
            BitmapFactory.decodeStream(istr).also { istr.close() }
        } catch (e: Exception) {
            null
        }
    }

    fun setBackground(gradientTextureBitmap: Bitmap) {
        backgroundBitmap = gradientTextureBitmap
    }

    fun destroy() {
        if (mProgramObject != 0) {
            GLES20.glDeleteProgram(mProgramObject)
            mProgramObject = 0
        }
    }
}
