package com.kotlinisgood.boomerang.ui.videodoodle

/**
 * This class essentially represents a viewport-sized sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 */
class FullFrameRect {
    private val rectDrawable: Drawable2d = Drawable2d()
    private var program = Texture2dProgram()



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
            GlUtil.IDENTITY_MATRIX, rectDrawable.vertexArray, 0,
            rectDrawable.vertexCount, rectDrawable.coordsPerVertex,
            rectDrawable.vertexStride,
            texMatrix, rectDrawable.texCoordArray, textureId,
            rectDrawable.texCoordStride
        )
    }
}