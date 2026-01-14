package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RadarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val v = RadarView(this)
        setContentView(v)

        // דמו blips (אפשר להחליף במידע אמיתי מהפידים/מקורות)
        v.setBlips(
            listOf(
                RadarBlip(0.25f, 0.8f),
                RadarBlip(0.55f, 2.2f),
                RadarBlip(0.85f, 4.6f)
            )
        )
    }
}
