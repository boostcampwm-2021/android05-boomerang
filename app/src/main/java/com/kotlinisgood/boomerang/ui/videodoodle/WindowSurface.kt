package com.kotlinisgood.boomerang.ui.videodoodle

import android.opengl.EGL14
import android.view.Surface

/**
 * EGLSurface 중 windowSurface (pixmap, pbuffer는 offscreen을 위한 surface)
 *
 */
class WindowSurface(private val egl: Egl, surface: Surface?) {
    private var eglSurface = EGL14.EGL_NO_SURFACE
    var width = -1
    var height = -1

    init {
        createWindowSurface(surface)
    }

    private fun createWindowSurface(surface: Surface?) {
        check(!(eglSurface !== EGL14.EGL_NO_SURFACE))
        eglSurface = egl.createWindowSurface(surface)

        width = egl.querySurface(eglSurface, EGL14.EGL_WIDTH)
        height = egl.querySurface(eglSurface, EGL14.EGL_HEIGHT)
    }

    fun makeCurrent() = egl.makeCurrent(eglSurface)

    fun makeCurrentReadFrom(readSurface: WindowSurface) =
        egl.makeCurrent(eglSurface, readSurface.eglSurface)

    fun swapBuffers() = egl.swapBuffers(eglSurface)

    fun setPresentationTime(nsecs: Long) = egl.setPresentationTime(eglSurface, nsecs)

    fun release() {
        egl.releaseSurface(eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}