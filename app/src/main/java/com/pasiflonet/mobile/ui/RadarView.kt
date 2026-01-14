package com.pasiflonet.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class RadarBlip(
    val angleDeg: Float,   // 0..360
    val radius01: Float,   // 0..1
    val label: String = ""
)

class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ui = Handler(Looper.getMainLooper())
    private var sweep = 0f

    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(120, 80, 255, 160)
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(200, 0, 255, 140)
    }

    private val blipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(230, 0, 255, 140)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 210, 255, 235)
        textSize = 26f
    }

    private var blips: List<RadarBlip> = emptyList()

    private val tick = object : Runnable {
        override fun run() {
            sweep = (sweep + 2.2f) % 360f
            invalidate()
            ui.postDelayed(this, 16L)
        }
    }

    fun setBlips(list: List<RadarBlip>) {
        blips = list.take(24)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ui.removeCallbacks(tick)
        ui.post(tick)
    }

    override fun onDetachedFromWindow() {
        ui.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) * 0.46f

        // circles
        canvas.drawCircle(cx, cy, r, grid)
        canvas.drawCircle(cx, cy, r * 0.66f, grid)
        canvas.drawCircle(cx, cy, r * 0.33f, grid)

        // cross
        canvas.drawLine(cx - r, cy, cx + r, cy, grid)
        canvas.drawLine(cx, cy - r, cx, cy + r, grid)

        // sweep line
        val rad = Math.toRadians(sweep.toDouble())
        val x = (cx + cos(rad) * r).toFloat()
        val y = (cy + sin(rad) * r).toFloat()
        canvas.drawLine(cx, cy, x, y, sweepPaint)

        // blips
        for (b in blips) {
            val a = Math.toRadians(b.angleDeg.toDouble())
            val rr = (b.radius01.coerceIn(0f, 1f) * r)
            val bx = (cx + cos(a) * rr).toFloat()
            val by = (cy + sin(a) * rr).toFloat()
            canvas.drawCircle(bx, by, 7f, blipPaint)
        }

        canvas.drawText("RADAR", 16f, (h - 16f), textPaint)
    }
}
