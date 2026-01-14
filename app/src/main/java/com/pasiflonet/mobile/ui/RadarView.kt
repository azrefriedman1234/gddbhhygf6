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

    // נתונים פשוטים: ערך 0..1 = “כמה חדש”, זווית 0..2π
    data class Blip(val r: Float, val a: Float)

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; style = Paint.Style.STROKE }
    private val blip = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    var blips: List<Blip> = emptyList()
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val rad = min(cx, cy) * 0.92f

        // טבעות
        for (i in 1..4) {
            c.drawCircle(cx, cy, rad * (i / 4f), bg)
        }
        // צלב
        c.drawLine(cx - rad, cy, cx + rad, cy, bg)
        c.drawLine(cx, cy - rad, cx, cy + rad, bg)

        for (b in blips) {
            val rr = rad * (1f - b.r.coerceIn(0f, 1f))
            val x = cx + rr * cos(b.a)
            val y = cy + rr * sin(b.a)
            c.drawCircle(x, y, 10f, blip)
        }
    }
}
