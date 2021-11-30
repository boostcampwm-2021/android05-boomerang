package com.kotlinisgood.boomerang.ui.videodoodle

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RectangleVertex {
    val vertexArray = createFloatBuffer(FULL_RECTANGLE_COORDS)
    val texCoordArray = createFloatBuffer(FULL_RECTANGLE_TEX_COORDS)
    val vertexCount = FULL_RECTANGLE_COORDS.size / COORDS_PER_VERTEX
    val vertexStride = COORDS_PER_VERTEX * SIZEOF_FLOAT
    val texCoordStride = 2 * SIZEOF_FLOAT

    private fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(coords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(coords)
        fb.position(0)
        return fb
    }

    companion object {
        const val COORDS_PER_VERTEX = 2
        private const val SIZEOF_FLOAT = 4
        private val FULL_RECTANGLE_COORDS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
    }
}