package com.pasiflonet.mobile.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class IntelFeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Intel Feed נמצא במסך הראשי (Dashboard)."
            textSize = 18f
        })
    }
}
