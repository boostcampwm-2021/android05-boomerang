package com.kotlinisgood.boomerang.ui.videodoodle

import android.opengl.EGL14
import android.util.Log
import android.view.Surface

/**
 * EGLSurface 중 WindowSurface (Pixmap, Pbuffer는 offscreen을 위한 surface)
 *
 *
 * It's good practice to explicitly release() the surface, preferably from a "finally" block.
 */
/**
 * Associates an EGL surface with the native window surface.
 *
 *
 * Set releaseSurface to true if you want the Surface to be released when release() is
 * called.  This is convenient, but can interfere with framework classes that expect to
 * manage the Surface themselves (e.g. if you release a SurfaceView's Surface, the
 * surfaceDestroyed() callback won't fire).
 */
class EglWindowSurface(private val egl: Egl, private var surface: Surface?) {
    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var mWidth = -1
    private var mHeight = -1

    init {
        createWindowSurface(surface)
    }

    /**
     * Creates a window surface.
     *
     *
     * @param surface May be a Surface or SurfaceTexture.
     */
    fun createWindowSurface(surface: Any?) {
        check(!(eglSurface !== EGL14.EGL_NO_SURFACE)) { "surface already created" }
        eglSurface = egl.createWindowSurface(surface!!)

        // Don't cache width/height here, because the size of the underlying surface can change
        // out from under us (see e.g. HardwareScalerActivity).
        mWidth = egl.querySurface(eglSurface, EGL14.EGL_WIDTH);
        mHeight = egl.querySurface(eglSurface, EGL14.EGL_HEIGHT);
    }

    /**
     * Returns the surface's width, in pixels.
     *
     *
     * If this is called on a window surface, and the underlying surface is in the process
     * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
     * callback).  The size should match after the next buffer swap.
     */
    val width: Int
        get() = if (mWidth < 0) {
            egl.querySurface(eglSurface, EGL14.EGL_WIDTH)
        } else {
            mWidth
        }

    /**
     * Returns the surface's height, in pixels.
     */
    val height: Int
        get() {
            return if (mHeight < 0) {
                egl.querySurface(eglSurface, EGL14.EGL_HEIGHT)
            } else {
                mHeight
            }
        }

    /**
     * Release the EGL surface.
     */
    fun releaseEglSurface() {
        egl.releaseSurface(eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
        mHeight = -1
        mWidth = mHeight
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        egl.makeCurrent(eglSurface)
    }

    /**
     * Makes our EGL context and surface current for drawing, using the supplied surface
     * for reading.
     */
    fun makeCurrentReadFrom(readSurface: EglWindowSurface) {
        egl.makeCurrent(eglSurface, readSurface.eglSurface)
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    fun swapBuffers(): Boolean {
        val result = egl.swapBuffers(eglSurface)
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed")
        } else {
            println("SUCCESS!!!!!")
        }
        return result
    }

    /**
     * Sends the presentation time stamp to EGL.
     *
     * @param nsecs Timestamp, in nanoseconds.
     */
    fun setPresentationTime(nsecs: Long) {
        egl.setPresentationTime(eglSurface, nsecs)
    }

    /**
     * Releases any resources associated with the EGL surface (and, if configured to do so,
     * with the Surface as well).
     *
     *
     * Does not require that the surface's EGL context be current.
     */
    fun release() {
        releaseEglSurface()
        if (surface != null) {
            surface = null
        }
    }

    companion object {
        private const val TAG: String = "EglSurfaceBase"
    }
}