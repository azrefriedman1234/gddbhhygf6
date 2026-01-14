package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pasiflonet.mobile.R
import kotlin.random.Random

class RadarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radar)

        val v: RadarView = findViewById(R.id.radarView)

        // כרגע דמו ויזואלי (חיבור לנתונים נעשה אחרי שנראה שהפרויקט מתקמפל)
        val blips = (1..40).map {
            RadarView.Blip(r = Random.nextFloat(), a = Random.nextFloat() * (Math.PI.toFloat() * 2f))
        }
        v.blips = blips
    }
}
