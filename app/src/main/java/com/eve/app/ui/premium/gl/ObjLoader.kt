package com.eve.app.ui.premium.gl

import android.content.Context
import java.io.DataInputStream
import java.io.IOException

class ObjLoader(context: Context, file: String, scale: Float) {
    var numFaces: Int = 0
    var normals: FloatArray = FloatArray(0)
    var textureCoordinates: FloatArray = FloatArray(0)
    var positions: FloatArray = FloatArray(0)

    init {
        try {
            val inputStream = DataInputStream(context.assets.open(file))
            var n = inputStream.readInt()
            val vertices = FloatArray(n)
            for (i in 0 until n) {
                vertices[i] = inputStream.readFloat()
            }

            n = inputStream.readInt()
            val textures = FloatArray(n)
            for (i in 0 until n) {
                textures[i] = inputStream.readFloat()
            }

            n = inputStream.readInt()
            val norms = FloatArray(n)
            for (i in 0 until n) {
                norms[i] = inputStream.readFloat()
            }

            n = inputStream.readInt()
            numFaces = n
            normals = FloatArray(numFaces * 3)
            textureCoordinates = FloatArray(numFaces * 2)
            positions = FloatArray(numFaces * 3)
            var positionIndex = 0
            var normalIndex = 0
            var textureIndex = 0

            for (i in 0 until n) {
                var index = 3 * inputStream.readInt()
                positions[positionIndex++] = vertices[index++] * scale
                positions[positionIndex++] = vertices[index++] * scale
                positions[positionIndex++] = vertices[index] * scale

                index = 2 * inputStream.readInt()
                textureCoordinates[normalIndex++] = if (index < 0 || index >= textures.size) 0f else textures[index]
                index++
                textureCoordinates[normalIndex++] = if (index < 0 || index >= textures.size) 0f else 1f - textures[index]

                index = 3 * inputStream.readInt()
                normals[textureIndex++] = norms[index++]
                normals[textureIndex++] = norms[index++]
                normals[textureIndex++] = norms[index]
            }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
