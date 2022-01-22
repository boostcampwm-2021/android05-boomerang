package com.kotlinisgood.boomerang.ui.videodoodle

import android.opengl.Matrix

class FullFrameRectangle {
    private val rectangleVertex = RectangleVertex()
    private var program = Program()
    private val matrix: FloatArray = FloatArray(16)

    init {
        Matrix.setIdentityM(matrix, 0)
    }

    fun createTextureObject() = program.createTextureObject()

    fun drawFrame(textureId: Int, texMatrix: FloatArray?) {
        program.draw(
            matrix, rectangleVertex.vertexArray, texMatrix, rectangleVertex.texCoordArray, textureId
        )
    }
}