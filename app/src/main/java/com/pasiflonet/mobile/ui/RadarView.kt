package com.pasiflonet.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var angle = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(w, h) * 0.45f
        val cx = w / 2f
        val cy = h / 2f

        // background grid
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.alpha = 90
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawCircle(cx, cy, r * 0.66f, paint)
        canvas.drawCircle(cx, cy, r * 0.33f, paint)

        // cross lines
        paint.alpha = 60
        canvas.drawLine(cx - r, cy, cx + r, cy, paint)
        canvas.drawLine(cx, cy - r, cx, cy + r, paint)

        // sweep
        paint.alpha = 180
        val rad = Math.toRadians(angle.toDouble())
        val x = (cx + r * cos(rad)).toFloat()
        val y = (cy + r * sin(rad)).toFloat()
        canvas.drawLine(cx, cy, x, y, paint)

        // animate
        angle += 2.2f
        if (angle >= 360f) angle = 0f
        postInvalidateOnAnimation()
    }
}
