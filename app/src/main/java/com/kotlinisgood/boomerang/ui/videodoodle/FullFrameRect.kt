package com.kotlinisgood.boomerang.ui.videodoodle

/**
 * This class essentially represents a viewport-sized sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 */
class FullFrameRect {
    private val mRectDrawable: Drawable2d = Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE)
    private var program = Texture2dProgram()

    /**
     * Releases resources.
     *
     *
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    fun release(doEglCleanup: Boolean) {
        if (doEglCleanup) program.release()
    }


    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    fun createTextureObject(): Int {
        return program.createTextureObject()
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    fun drawFrame(textureId: Int, texMatrix: FloatArray?) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        program.draw(
            GlUtil.IDENTITY_MATRIX, mRectDrawable.vertexArray, 0,
            mRectDrawable.vertexCount, mRectDrawable.coordsPerVertex,
            mRectDrawable.vertexStride,
            texMatrix, mRectDrawable.texCoordArray, textureId,
            mRectDrawable.texCoordStride
        )
    }
}