package com.kotlinisgood.boomerang.ui.videodoodle

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout

/**
 * SurfaceView 의 Layout 상에서 위치와 크기, 비율을 잡아주는 FrameLayout
 */
class AspectFrameLayout : FrameLayout {
    private var targetAspect = -1.0

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    fun setAspectRatio(aspectRatio: Double) {
        require(aspectRatio >= 0)
        if (targetAspect != aspectRatio) {
            targetAspect = aspectRatio
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

        if (targetAspect > 0) {
            var initialWidth = MeasureSpec.getSize(exactWidthMeasureSpec)
            var initialHeight = MeasureSpec.getSize(exactHeightMeasureSpec)

            val horizontalPadding = paddingLeft + paddingRight
            val verticalPadding = paddingTop + paddingBottom
            initialWidth -= horizontalPadding
            initialHeight -= verticalPadding
            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = targetAspect / viewAspectRatio - 1
            if (aspectDiff > 0) {
                val preHeight = (initialWidth / targetAspect).toInt()
                initialHeight = preHeight - (preHeight % 16)
            } else {
                val preWidth = (initialHeight * targetAspect).toInt()
                initialWidth = preWidth - (preWidth % 16)
            }
            Log.d(
                TAG,
                "Adjust size to multiple of 16. Width: $initialWidth, Height: $initialHeight"
            )
            initialWidth += horizontalPadding
            initialHeight += verticalPadding
            exactWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
            exactHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(exactWidthMeasureSpec, exactHeightMeasureSpec)
    }

    companion object {
        private const val TAG = "AspectFrameLayout"
    }
}