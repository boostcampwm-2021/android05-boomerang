package com.kotlinisgood.boomerang.ui.videodoodlelight

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View


class DrawView(context: Context) : View(context) {
    var points = mutableListOf<Point>()
    val p = Paint()
    var color = Color.RED

    override fun onDraw(canvas: Canvas) {
        p.color = color
        p.strokeWidth = 5F
        for (i in 1 until points.size) {
            if (!points.get(i).check)
                continue
            canvas.drawLine(
                points.get(i - 1).x,
                points.get(i - 1).y,
                points.get(i).x,
                points.get(i).y,
                p
            )
        }
    }
}

data class Point(var x: Float, var y: Float, var check: Boolean)