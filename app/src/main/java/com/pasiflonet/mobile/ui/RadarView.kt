package com.pasiflonet.mobile.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {

    data class Blip(
        val label: String,
        val angleDeg: Float,
        val radius: Float,   // 0..1
        val strength: Float  // 0..1
    )

    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 160
    }
    private val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        alpha = 190
    }
    private val blip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 220
    }

    private var sweepAngle = 0f
    private var animator: ValueAnimator? = null
    private var blips: List<Blip> = emptyList()

    fun setBlips(list: List<Blip>) {
        blips = list
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 2200L
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    sweepAngle = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(w, h) * 0.48f
        val cx = w / 2f
        val cy = h / 2f

        // “Cyber” צבע בסיסי (ירקרק/טורקיז) – בלי תלות ב-resources
        grid.setARGB(255, 0, 255, 170)
        sweep.setARGB(255, 0, 255, 170)
        blip.setARGB(255, 0, 255, 170)

        // circles
        for (k in 1..4) {
            c.drawCircle(cx, cy, r * (k / 4f), grid)
        }
        // cross lines
        c.drawLine(cx - r, cy, cx + r, cy, grid)
        c.drawLine(cx, cy - r, cx, cy + r, grid)

        // sweep
        val ang = Math.toRadians(sweepAngle.toDouble())
        val sx = cx + cos(ang).toFloat() * r
        val sy = cy + sin(ang).toFloat() * r
        c.drawLine(cx, cy, sx, sy, sweep)

        // blips
        for (b in blips) {
            val a = Math.toRadians(b.angleDeg.toDouble())
            val rr = r * b.radius.coerceIn(0f, 1f)
            val x = cx + cos(a).toFloat() * rr
            val y = cy + sin(a).toFloat() * rr
            blip.alpha = (120 + (135 * b.strength.coerceIn(0f, 1f))).toInt()
            c.drawCircle(x, y, 6f + 8f * b.strength, blip)
        }
    }
}
