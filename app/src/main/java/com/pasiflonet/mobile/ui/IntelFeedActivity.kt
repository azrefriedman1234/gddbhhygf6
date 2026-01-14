package com.pasiflonet.mobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.intel.IntelItem
import com.pasiflonet.mobile.intel.RssParser
import com.pasiflonet.mobile.intel.TranslateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IntelFeedActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private lateinit var etUrl: EditText
    private lateinit var swTranslate: Switch
    private lateinit var rv: RecyclerView
    private val items = ArrayList<IntelItem>()
    private val adapter = IntelFeedAdapter(items) { item ->
        val url = item.link
        if (url.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intel_feed)

        etUrl = findViewById(R.id.etFeedUrl)
        swTranslate = findViewById(R.id.swTranslate)
        rv = findViewById(R.id.rv)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val prefs = getSharedPreferences("intel_prefs", Context.MODE_PRIVATE)
        val defUrl = prefs.getString("feed_url", "") ?: ""
        etUrl.setText(defUrl)
        swTranslate.isChecked = prefs.getBoolean("translate", true)

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("feed_url", etUrl.text.toString().trim())
                .putBoolean("translate", swTranslate.isChecked)
                .apply()
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            refresh()
        }

        // רענון ראשון
        refresh()
    }

    private fun refresh() {
        val url = etUrl.text.toString().trim()
        if (url.isBlank()) return

        scope.launch {
            val translateOn = swTranslate.isChecked
            val translator = if (translateOn) TranslateHelper(this@IntelFeedActivity) else null
            if (translator != null) translator.ensureModel()

            val newItems = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body ?: return@use emptyList()
                    body.byteStream().use { ins ->
                        RssParser.parse(ins, source = "X/RSS")
                    }
                }
            }

            items.clear()
            items.addAll(newItems.sortedByDescending { it.pubMillis })

            if (translator != null) {
                // תרגום מהיר רק לכותרות הראשונות כדי לא לתקוע UI
                scope.launch {
                    val top = items.take(25)
                    for (it in top) {
                        if (it.titleHe == null) {
                            it.titleHe = translator.enToHe(it.title)
                            adapter.notifyDataSetChanged()
                        }
                    }
                    translator.close()
                }
            }

            adapter.notifyDataSetChanged()
        }
    }
}
