package com.kotlinisgood.boomerang.ui.videodoodle

import java.nio.FloatBuffer

/**
 * Base class for stuff we like to draw.
 */
class Drawable2d {
    /**
     * Returns the array of vertices.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    var vertexArray: FloatBuffer? = null

    /**
     * Returns the array of texture coordinates.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    var texCoordArray: FloatBuffer? = null

    /**
     * Returns the number of vertices stored in the vertex array.
     */
    var vertexCount = 0

    /**
     * Returns the number of position coordinates per vertex.  This will be 2 or 3.
     */
    var coordsPerVertex = 0

    /**
     * Returns the width, in bytes, of the data for each vertex.
     */
    var vertexStride = 0

    /**
     * Returns the width, in bytes, of the data for each texture coordinate.
     */
    val texCoordStride: Int

    companion object {
        private const val SIZEOF_FLOAT = 4

        /**
         * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
         * matrix is identity, this will exactly cover the viewport.
         *
         *
         * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
         * right with external textures from SurfaceTexture.)
         */
        private val FULL_RECTANGLE_COORDS = floatArrayOf(
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f
        )
        private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,  // 0 bottom left
            1.0f, 0.0f,  // 1 bottom right
            0.0f, 1.0f,  // 2 top left
            1.0f, 1.0f // 3 top right
        )
        private val FULL_RECTANGLE_BUF = GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS)
        private val FULL_RECTANGLE_TEX_BUF = GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS)
    }

    /**
     * Prepares a drawable from a "pre-fabricated" shape definition.
     *
     *
     * Does no EGL/GL operations, so this can be done at any time.
     */
    init {
        vertexArray = FULL_RECTANGLE_BUF
        texCoordArray = FULL_RECTANGLE_TEX_BUF
        coordsPerVertex = 2
        vertexStride = coordsPerVertex * SIZEOF_FLOAT
        vertexCount = FULL_RECTANGLE_COORDS.size / coordsPerVertex

        texCoordStride = 2 * SIZEOF_FLOAT
    }
}