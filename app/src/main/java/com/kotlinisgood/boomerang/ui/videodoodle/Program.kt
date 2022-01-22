package com.kotlinisgood.boomerang.ui.videodoodle

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.FloatBuffer

class Program {
    private var programHandle = createProgram()
    private val mvpMatrixLoc: Int
    private val texMatrixLoc: Int
    private var kernelLoc: Int
    private var texOffsetLoc = 0
    private var colorAdjustLoc = 0
    private val positionLoc: Int
    private val textureCoordLoc: Int
    private lateinit var texOffset: FloatArray

    init {
        if (programHandle == 0) throw Exception("Unable to create program")

        positionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition")
        textureCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord")
        mvpMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        texMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix")
        kernelLoc = GLES20.glGetUniformLocation(programHandle, "uKernel")
        if (kernelLoc < 0) {
            kernelLoc = -1
            texOffsetLoc = -1
            colorAdjustLoc = -1
        } else {
            texOffsetLoc = GLES20.glGetUniformLocation(programHandle, "uTexOffset")
            colorAdjustLoc = GLES20.glGetUniformLocation(programHandle, "uColorAdjust")

            val rw = 1.0f / 256
            val rh = 1.0f / 256

            texOffset = floatArrayOf(
                -rw, -rh, 0f, -rh, rw, -rh,
                -rw, 0f, 0f, 0f, rw, 0f,
                -rw, rh, 0f, rh, rw, rh
            )
        }
    }

    private fun createProgram(): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (pixelShader == 0) {
            return 0
        }
        var program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Could not create program")
        }
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, pixelShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program")
            Log.e(TAG, GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    private fun loadShader(shaderType: Int, source: String?): Int {
        var shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    fun createTextureObject(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val texId = textures[0]
        GLES20.glBindTexture(textureTarget, texId)
        GLES20.glTexParameteri(
            textureTarget, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            textureTarget, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        return texId
    }

    fun draw(
        mvpMatrix: FloatArray?,
        vertexBufferOld: FloatBuffer?,
        texMatrix: FloatArray?,
        texBufferOld: FloatBuffer?,
        textureId: Int
    ) {
        GLES20.glUseProgram(programHandle)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(textureTarget, textureId)

        GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, texMatrix, 0)

        val vertexBuffer = IntArray(1)
        val texBuffer = IntArray(1)
        GLES20.glGenBuffers(1, vertexBuffer, 0)
        GLES20.glGenBuffers(1, texBuffer, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 32, vertexBufferOld, GLES20.GL_STATIC_DRAW)
        GLES20.glEnableVertexAttribArray(positionLoc)
        GLES20.glVertexAttribPointer(
            positionLoc, 2, GLES20.GL_FLOAT, false, 8, 0
        )

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texBuffer[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 32, texBufferOld, GLES20.GL_STATIC_DRAW)
        GLES20.glEnableVertexAttribArray(textureCoordLoc)
        GLES20.glVertexAttribPointer(
            textureCoordLoc, 2, GLES20.GL_FLOAT, false, 8, 0
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glBindTexture(textureTarget, 0)
        GLES20.glUseProgram(0)
    }

    fun release() {
        GLES20.glDeleteProgram(programHandle)
        programHandle = -1
    }

    companion object {
        private const val VERTEX_SHADER = ("uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uTexMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                "}\n")

        private const val FRAGMENT_SHADER =
            ("#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n")

        private const val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        private const val TAG = "GLProgram"
    }
}