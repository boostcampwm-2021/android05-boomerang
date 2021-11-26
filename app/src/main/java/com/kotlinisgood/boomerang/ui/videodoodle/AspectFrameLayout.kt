package com.kotlinisgood.boomerang.ui.videodoodle

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout

/**
 * Layout that adjusts to maintain a specific aspect ratio.
 */
class AspectFrameLayout : FrameLayout {
    private var mTargetAspect = -1.0 // initially use default window size

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    /**
     * Sets the desired aspect ratio.  The value is `width / height`.
     */
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var exactWidthMeasureSpec = widthMeasureSpec
        var exactHeightMeasureSpec = heightMeasureSpec
        Log.d(
            TAG, "onMeasure target=" + mTargetAspect +
                    " width=[" + MeasureSpec.toString(exactWidthMeasureSpec) +
                    "] height=[" + MeasureSpec.toString(exactHeightMeasureSpec) + "]"
        )

        // Target aspect ratio will be < 0 if it hasn't been set yet.  In that case,
        // we just use whatever we've been handed.
        if (mTargetAspect > 0) {
            var initialWidth = MeasureSpec.getSize(exactWidthMeasureSpec)
            var initialHeight = MeasureSpec.getSize(exactHeightMeasureSpec)

            // factor the padding out
            val horizPadding = paddingLeft + paddingRight
            val vertPadding = paddingTop + paddingBottom
            initialWidth -= horizPadding
            initialHeight -= vertPadding
            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = mTargetAspect / viewAspectRatio - 1
            if (Math.abs(aspectDiff) < 0.01) {
                // We're very close already.  We don't want to risk switching from e.g. non-scaled
                // 1280x720 to scaled 1280x719 because of some floating-point round-off error,
                // so if we're really close just leave it alone.
                Log.d(
                    TAG, "aspect ratio is good (target=" + mTargetAspect +
                            ", view=" + initialWidth + "x" + initialHeight + ")"
                )
            } else {
                if (aspectDiff > 0) {
                    // limited by narrow width; restrict height
                    initialHeight = (initialWidth / mTargetAspect).toInt()
                } else {
                    // limited by short height; restrict width
                    initialWidth = (initialHeight * mTargetAspect).toInt()
                }
                Log.d(
                    TAG, "new size=" + initialWidth + "x" + initialHeight + " + padding " +
                            horizPadding + "x" + vertPadding
                )
                initialWidth += horizPadding
                initialHeight += vertPadding
                exactWidthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                exactHeightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
            }
        }

        //Log.d(TAG, "set width=[" + MeasureSpec.toString(widthMeasureSpec) +
        //        "] height=[" + View.MeasureSpec.toString(heightMeasureSpec) + "]");
//        super.onMeasure(exactWidthMeasureSpec, exactHeightMeasureSpec)
        super.onMeasure(exactWidthMeasureSpec, exactHeightMeasureSpec)
    }

    companion object {
        private const val TAG: String = "AspectFrameLayout"
    }
}