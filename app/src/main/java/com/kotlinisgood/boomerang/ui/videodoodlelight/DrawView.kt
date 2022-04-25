package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class DrawView(context: Context?) : View(context) {
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private var path: Path = Path()
    private var bitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var paint: Paint = Paint()

    private var mX = 0f
    private  var mY:Float = 0f
    private val tolerance = 4f

    init {
        paint.color = -0x10000
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 10.0f
        setBackgroundColor(0x00000000)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
        canvas.drawPath(path, paint)
    }


    private fun touchStart(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy: Float = abs(y - mY)
        if (dx >= tolerance || dy >= tolerance) {
            path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }

    private fun touchUp() {
        path.lineTo(mX, mY)
        // commit the path to our offscreen
        canvas.drawPath(path, paint)
        // kill this so we don't double draw
        path.reset()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                performClick()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        println()
        return super.performClick()
    }

    fun setColor(color: Int) {
        paint.color = color
    }
}