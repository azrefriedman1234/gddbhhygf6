package com.pasiflonet.mobile.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.rss.RssSourceStore

class RssSourcesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rss_sources)

        val et = findViewById<EditText>(R.id.etRssSources)
        val btnSave = findViewById<Button>(R.id.btnSaveRss)
        val btnReset = findViewById<Button>(R.id.btnResetRss)

        et.setText(RssSourceStore.loadText(this))

        btnSave.setOnClickListener {
            RssSourceStore.saveText(this, et.text?.toString().orEmpty())
            Toast.makeText(this, "✅ נשמר", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnReset.setOnClickListener {
            et.setText(RssSourceStore.defaultText())
            Toast.makeText(this, "↩️ הוחזר לברירת מחדל (לא נשמר עדיין)", Toast.LENGTH_SHORT).show()
        }
    }
}
