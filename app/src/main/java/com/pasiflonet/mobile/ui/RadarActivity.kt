package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RadarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(RadarView(this))
    }
}
