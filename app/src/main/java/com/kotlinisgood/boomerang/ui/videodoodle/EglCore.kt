package com.kotlinisgood.boomerang.ui.videodoodle

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface

class EglCore {
    private lateinit var eglDisplay: EGLDisplay

    //    private var eglContext = EGL14.EGL_NO_CONTEXT
    private lateinit var eglContext: EGLContext
    private var eglConfig: EGLConfig? = null

    /**
     * Returns the GLES version this context is configured for (currently 2 or 3).
     */
    var glVersion = -1

    init {
        getEglDisplay()
        eglInit()
        eglBind()
        getEglContext()

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(
            eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
            values, 0
        )
        Log.d(TAG, "EGLContext created, client version " + values[0])
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  This must be
     * called from the thread where the context was created.
     *
     *
     * On completion, no context will be current.
     */
    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
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
//        eglConfig = null
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                // We're limited here -- finalizers don't run on the thread that holds
                // the EGL state, so if a surface or context is still current on another
                // thread we can't fully release it here.  Exceptions thrown from here
                // are quietly discarded.  Complain in the log file.
                Log.w(TAG, "WARNING: EglCore was not explicitly released -- state may be leaked")
                release()
            }
        } finally {
//            super.finalize()
        }
    }

    /**
     * Destroys the specified surface.  Note the EGLSurface won't actually be destroyed if it's
     * still current in a context.
     */
    fun releaseSurface(eglSurface: EGLSurface?) {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
    }

    /**
     * Creates an EGL surface associated with a Surface.
     *
     *
     * If this is destined for MediaCodec, the EGLConfig should have the "recordable" attribute.
     */
    fun createWindowSurface(surface: Any): EGLSurface {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw Exception("invalid surface: $surface")
        }

        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, eglConfig, surface,
            surfaceAttribs, 0
        )
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == null) {
            throw Exception("surface was null")
        }
        return eglSurface
    }

    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(
            eglDisplay, eglConfig,
            surfaceAttribs, 0
        )
        checkEglError("eglCreatePbufferSurface")
        if (eglSurface == null) {
            throw Exception("surface was null")
        }
        return eglSurface
    }

    /**
     * Makes our EGL context current, using the supplied surface for both "draw" and "read".
     */
    fun makeCurrent(eglSurface: EGLSurface?) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) throw Exception("EGL Current 설정 실패")
    }

    /**
     * Makes our EGL context current, using the supplied "draw" and "read" surfaces.
     */
    fun makeCurrent(drawSurface: EGLSurface?, readSurface: EGLSurface?) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, drawSurface, readSurface, eglContext)) {
            throw Exception("eglMakeCurrent(draw,read) failed")
        }
    }

    /**
     * Makes no context current.
     */
    fun makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        ) {
            throw Exception("eglMakeCurrent failed")
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    fun swapBuffers(eglSurface: EGLSurface?): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    fun setPresentationTime(eglSurface: EGLSurface?, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    /**
     * Returns true if our context and the specified surface are current.
     */
    fun isCurrent(eglSurface: EGLSurface): Boolean {
        return eglContext == EGL14.eglGetCurrentContext() && eglSurface == EGL14.eglGetCurrentSurface(
            EGL14.EGL_DRAW
        )
    }

    /**
     * Performs a simple surface query.
     */
    fun querySurface(eglSurface: EGLSurface?, what: Int): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, 0)
        return value[0]
    }

    /**
     * Queries a string value.
     */
    fun queryString(what: Int): String {
        return EGL14.eglQueryString(eglDisplay, what)
    }

    /**
     * Checks for EGL errors.  Throws an exception if an error has been raised.
     */
    private fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw Exception(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }

    companion object {
        private const val TAG: String = "EglCoreTAG"

        /**
         * Constructor flag: surface must be recordable.  This discourages EGL from using a
         * pixel format that cannot be converted efficiently to something usable by the video
         * encoder.
         */
        const val FLAG_RECORDABLE = 0x01

        /**
         * Constructor flag: ask for GLES3, fall back to GLES2 if not available.  Without this
         * flag, GLES2 is used.
         */
        const val FLAG_TRY_GLES3 = 0x02

        // Android-specific extension.
        private const val EGL_RECORDABLE_ANDROID = 0x3142

        /**
         * Writes the current display, context, and surface to the log.
         */
        fun logCurrent(msg: String) {
            val display: EGLDisplay
            val context: EGLContext
            val surface: EGLSurface
            display = EGL14.eglGetCurrentDisplay()
            context = EGL14.eglGetCurrentContext()
            surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
            Log.i(
                TAG, "Current EGL (" + msg + "): display=" + display + ", context=" + context +
                        ", surface=" + surface
            )
        }
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

    /**
     * Finds a suitable EGLConfig.
     *
     * @param version Must be 2 or 3.
     */
    private fun getEglConfig(version: Int) {
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,  //EGL14.EGL_DEPTH_SIZE, 16,
            //EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            EGL14.EGL_NONE, 0,  // placeholder for recordable [@-3]
            EGL14.EGL_NONE
        )
        attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID
        attribList[attribList.size - 2] = 1
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                attribList,
                0,
                configs,
                0,
                configs.size,
                numConfigs,
                0
            )
        ) {
            Log.d(TAG, "$version EGLConfig 실패")
            eglConfig = null
        }
        eglConfig = configs[0]
//        return configs[0]
//        if (eglConfig == null) getEglConfig(2)
    }

    /**
     * OS의 WindowManager가 할 수 있는 API 목록을 EGLConfig를 통해 전달
     * 우선 OpenGL ES version 3을 시도하고, 실패시 2를 시도. 이마저 실패하면 Exception
     */
    private fun getEglContext() {
        getEglConfig(3)
        if (eglConfig != null) {
            val attrib3_list = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            val context = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                attrib3_list, 0
            )
            if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                Log.d(TAG, "Got GLES 3 config");
                eglContext = context
                glVersion = 3
            }
        } else {
            getEglConfig(2)
            if(eglConfig == null) throw Exception("Unable to find a suitable EGLConfig")
            val attrib2_list = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            val context = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                attrib2_list, 0
            )
            checkEglError("eglCreateContext")
            Log.d(TAG, "Got GLES 2 config");
            eglContext = context
            glVersion = 2
        }
    }
}