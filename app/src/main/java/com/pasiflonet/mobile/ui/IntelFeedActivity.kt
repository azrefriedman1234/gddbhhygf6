package com.pasiflonet.mobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.intel.TranslateHelper

class IntelFeedActivity : AppCompatActivity() {

    data class IntelRow(
        val title: String,
        val translated: String = "",
        val link: String = ""
    )

    private lateinit var rv: RecyclerView
    private val adapter = IntelFeedAdapter { any ->
        val link = extractLink(any)
        if (link.isNotBlank()) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@IntelFeedActivity)
            adapter = this@IntelFeedActivity.adapter
        }
        setContentView(rv)

        // תאימות: הקוד הישן חיפש ensureModel/enToHe/close
        TranslateHelper.ensureModel(this)

        // כרגע נותן שורה לדוגמה כדי שלא יהיה מסך ריק, וזה מתקמפל תמיד
        val demo = listOf(
            IntelRow(
                title = "Intel feed enabled ✅ (RSS/alt sources can be wired next)",
                translated = "",
                link = ""
            )
        )
        adapter.submit(demo)
    }

    override fun onDestroy() {
        // תאימות לקוד שהיה
        TranslateHelper.close()
        super.onDestroy()
    }

    private fun extractLink(obj: Any): String {
        // 1) אם זה IntelRow
        if (obj is IntelRow) return obj.link

        // 2) נסיון רפלקציה: link/url/href/permalink
        val c = obj.javaClass
        val names = listOf("link", "url", "href", "permalink")
        for (n in names) {
            try {
                val getter = "get" + n.replaceFirstChar { it.uppercase() }
                val m = c.methods.firstOrNull { it.parameterTypes.isEmpty() && (it.name == getter || it.name == n) }
                val v = m?.invoke(obj)?.toString()?.trim().orEmpty()
                if (v.isNotEmpty() && v != "null") return v
            } catch (_: Exception) {}
            try {
                val f = c.declaredFields.firstOrNull { it.name == n }
                if (f != null) {
                    f.isAccessible = true
                    val v = f.get(obj)?.toString()?.trim().orEmpty()
                    if (v.isNotEmpty() && v != "null") return v
                }
            } catch (_: Exception) {}
        }
        return ""
    }
}
