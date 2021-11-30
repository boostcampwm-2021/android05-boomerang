package com.kotlinisgood.boomerang.ui.videodoodle

import android.opengl.*
import android.util.Log
import android.view.Surface

class Egl {
    private lateinit var eglDisplay: EGLDisplay
    private lateinit var eglContext: EGLContext
    private var eglConfig: EGLConfig? = null

    var glVersion = -1

    init {
        getEglDisplay()
        eglInit()
        eglBind()
        getEglContext()

        val values = IntArray(1)
        EGL14.eglQueryContext(
            eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
            values, 0
        )
    }

    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
    }

    fun releaseSurface(eglSurface: EGLSurface?) {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
    }

    fun createWindowSurface(surface: Surface?): EGLSurface {
        val surfaceAttributes = intArrayOf(EGL14.EGL_NONE)
        return EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttributes, 0)
    }

    fun makeCurrent(eglSurface: EGLSurface?) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw Exception("EGL Current 설정 실패")
        }
    }

    fun makeCurrent(drawSurface: EGLSurface?, readSurface: EGLSurface?) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, drawSurface, readSurface, eglContext)) {
            throw Exception("eglMakeCurrent(draw,read) failed")
        }
    }

    fun swapBuffers(eglSurface: EGLSurface?): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun setPresentationTime(eglSurface: EGLSurface?, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    fun querySurface(eglSurface: EGLSurface?, what: Int): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, 0)
        return value[0]
    }

    /**
     * EGLDisplay 가져오기
     * 렌더링 할 수 있는 하드웨어 리소스를 주어서 EGL이 하드웨어의 API를 사용할 수 있도록 하자
     * EGL_DEFAULT_DISPLAY: OS가 기본으로 제공하는 디스플레이
     */
    private fun getEglDisplay() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) throw Exception("EGLDisplay 가져오기 실패")
    }

    /**
     * EGL 초기화
     * eglInitialize(EGLDisplay, EGL 메이저 버전, EGL 마이너 버전)
     * T/F로 초기화 성공 여부 반환
     */
    private fun eglInit() {
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 0)) throw Exception("EGL 초기화 실패")
    }

    /**
     * EGL 렌더링 API 설정 (OPENGL, OPENGL_ES, OPENVG 등이 있음)
     * T/F로 설정 성공 여부 반환
     */
    private fun eglBind() {
        if (!EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)) throw Exception("EGL 렌더링 API 설정 실패")
    }

    private fun getEglConfig(version: Int) {
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)) {
            Log.d(TAG, "$version EGLConfig 실패")
        }
        eglConfig = configs[0]
    }

    /**
     * OS의 WindowManager가 할 수 있는 API 목록을 EGLConfig를 통해 전달
     * 우선 OpenGL ES version 3을 시도하고, 실패시 2를 시도. 이마저 실패하면 Exception
     */
    private fun getEglContext() {
        getEglConfig(3)
        if (eglConfig != null) {
            val gl3Setup = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                gl3Setup, 0
            )
            if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                Log.d(TAG, "GLES3 Config")
                eglContext = context
                glVersion = 3
            }
        } else {
            getEglConfig(2)
            val gl2Setup = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                gl2Setup, 0
            )
            Log.d(TAG, "GLES2 Config")
            eglContext = context
            glVersion = 2
        }
    }

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
        private const val TAG = "Egl"
    }
}