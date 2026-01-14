package com.pasiflonet.mobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.intel.TranslateHelper
import java.net.HttpURLConnection
import java.net.URL
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class IntelFeedActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private val adapter = IntelFeedAdapter { item ->
        item.link?.let {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } catch (_: Exception) {}
        }
    }

    // כאן אתה יכול לשים RSS/Atom חינמיים (אפשר להוסיף גם RSS-bridge של X אם יש לך קישור)
    private val feedUrls = listOf(
        "https://www.navy.mil/DesktopModules/ArticleCS/RSS.ashx?ContentType=1&Site=1",
        "https://www.centcom.mil/RSS/Press-Releases/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rv = RecyclerView(this)
        setContentView(rv)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        Thread {
            val items = fetchRss(feedUrls)
            val out = ArrayList<IntelItem>()
            // תרגום כותרות (best-effort)
            val lock = Object()
            var remaining = items.size
            if (remaining == 0) runOnUiThread { adapter.setItems(out) }

            items.forEach { it0 ->
                TranslateHelper.enToHe(this, it0.title) { he ->
                    synchronized(lock) {
                        out.add(it0.copy(titleHe = he))
                        remaining--
                        if (remaining == 0) {
                            out.sortByDescending { it.publishedAt }
                            runOnUiThread { adapter.setItems(out) }
                        }
                    }
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        TranslateHelper.close()
    }

    private fun fetchRss(urls: List<String>): List<IntelItem> {
        val all = ArrayList<IntelItem>()
        urls.forEach { u ->
            try {
                val xml = download(u)
                all.addAll(parseRss(xml, source = u))
            } catch (_: Exception) {}
        }
        return all
    }

    private fun download(u: String): String {
        val conn = URL(u).openConnection() as HttpURLConnection
        conn.connectTimeout = 12000
        conn.readTimeout = 12000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.inputStream.bufferedReader().use { return it.readText() }
    }

    private fun parseRss(xml: String, source: String): List<IntelItem> {
        val out = ArrayList<IntelItem>()
        val f = XmlPullParserFactory.newInstance()
        f.isNamespaceAware = true
        val p = f.newPullParser()
        p.setInput(xml.reader())

        var inItem = false
        var title: String? = null
        var link: String? = null

        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (p.name.lowercase()) {
                        "item", "entry" -> { inItem = true; title = null; link = null }
                        "title" -> if (inItem) title = p.nextText()
                        "link" -> if (inItem) {
                            val href = p.getAttributeValue(null, "href")
                            link = href ?: p.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (p.name.lowercase() == "item" || p.name.lowercase() == "entry") {
                        inItem = false
                        if (!title.isNullOrBlank()) {
                            out.add(IntelItem(title = title!!, link = link, source = source, publishedAt = System.currentTimeMillis()))
                        }
                    }
                }
            }
            event = p.next()
        }
        return out
    }
}
