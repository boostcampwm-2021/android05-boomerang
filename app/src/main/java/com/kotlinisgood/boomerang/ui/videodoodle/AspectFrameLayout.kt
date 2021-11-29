package com.kotlinisgood.boomerang.ui.videodoodle

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * SurfaceView 의 Layout 상에서 위치와 크기, 비율을 잡아주는 FrameLayout
 */
class AspectFrameLayout : FrameLayout {
    private var mTargetAspect = -1.0

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    fun setAspectRatio(aspectRatio: Double) {
        require(aspectRatio >= 0)
        Log.d(
            TAG,
            "Setting aspect ratio to $aspectRatio (was $mTargetAspect)"
        )
        if (mTargetAspect != aspectRatio) {
            mTargetAspect = aspectRatio
            requestLayout()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var exactWidthMeasureSpec = widthMeasureSpec
        var exactHeightMeasureSpec = heightMeasureSpec
        println(exactWidthMeasureSpec)
        println(exactHeightMeasureSpec)
        Log.d(
            TAG, "onMeasure target=" + mTargetAspect +
                    " width=[" + MeasureSpec.toString(exactWidthMeasureSpec) +
                    "] height=[" + MeasureSpec.toString(exactHeightMeasureSpec) + "]"
        )

        if (mTargetAspect > 0) {
            var initialWidth = MeasureSpec.getSize(exactWidthMeasureSpec)
            var initialHeight = MeasureSpec.getSize(exactHeightMeasureSpec)

            val horizPadding = paddingLeft + paddingRight
            val vertPadding = paddingTop + paddingBottom
            initialWidth -= horizPadding
            initialHeight -= vertPadding
            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = mTargetAspect / viewAspectRatio - 1
            if (abs(aspectDiff) < 0.01) {
                Log.d(
                    TAG, "aspect ratio is good (target=" + mTargetAspect +
                            ", view=" + initialWidth + "x" + initialHeight + ")"
                )
            } else {
                if (aspectDiff > 0) {
                    val preHeight = (initialWidth / mTargetAspect).toInt()
                    initialHeight = preHeight - (preHeight % 16)
                } else {
                    val preWidth = (initialHeight * mTargetAspect).toInt()
                    initialWidth = preWidth - (preWidth % 16)
                }
                Log.d(
                    TAG, "new size=" + initialWidth + "x" + initialHeight + " + padding " +
                            horizPadding + "x" + vertPadding
                )
                initialWidth += horizPadding
                initialHeight += vertPadding
                exactWidthMeasureSpec =
                    MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                exactHeightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
            }
        }
        super.onMeasure(exactWidthMeasureSpec, exactHeightMeasureSpec)
    }

    companion object {
        private const val TAG: String = "AspectFrameLayout"
    }
}