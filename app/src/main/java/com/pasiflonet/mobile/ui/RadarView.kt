package com.pasiflonet.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class RadarBlip(val relR: Float, val angleRad: Float)

class RadarView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 60; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 120; strokeWidth = 4f }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 220; style = Paint.Style.FILL }

    private var angle = 0f
    private var blips: List<RadarBlip> = emptyList()

    fun setBlips(list: List<RadarBlip>) {
        blips = list
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = min(cx, cy) * 0.95f

        // rings
        c.drawCircle(cx, cy, r * 0.33f, grid)
        c.drawCircle(cx, cy, r * 0.66f, grid)
        c.drawCircle(cx, cy, r, grid)

        // sweep line
        val a = angle
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        c.drawLine(cx, cy, x, y, sweep)

        // blips
        for (b in blips) {
            val rr = (b.relR.coerceIn(0f, 1f)) * r
            val bx = cx + cos(b.angleRad) * rr
            val by = cy + sin(b.angleRad) * rr
            c.drawCircle(bx, by, 6f, dot)
        }

        // animate
        angle += 0.08f
        postInvalidateOnAnimation()
    }
}
