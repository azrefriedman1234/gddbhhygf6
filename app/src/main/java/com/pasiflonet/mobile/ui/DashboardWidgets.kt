package com.pasiflonet.mobile.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.intel.TranslateHelper

object DashboardWidgets {

    fun attach(activity: Activity) {
        val radar = activity.findViewById<RadarView?>(R.id.radarView)
        val rvX = activity.findViewById<RecyclerView?>(R.id.rvX)

        // Radar demo – אפשר להחליף לבליפים לפי "חומרה" / קטגוריות
        radar?.setBlips(
            listOf(
                RadarBlip(0.20f, 1.0f),
                RadarBlip(0.48f, 2.6f),
                RadarBlip(0.78f, 4.1f)
            )
        )

        if (rvX != null) {
            rvX.layoutManager = LinearLayoutManager(activity)
            val ad = IntelFeedAdapter { item ->
                item.link?.let {
                    try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } catch (_: Exception) {}
                }
            }
            rvX.adapter = ad

            // פיד חינמי (RSS/Atom). אם יש לך RSS-bridge של X — אפשר להוסיף כאן URL.
            val feedUrls = listOf(
                "https://www.navy.mil/DesktopModules/ArticleCS/RSS.ashx?ContentType=1&Site=1",
                "https://www.centcom.mil/RSS/Press-Releases/"
            )

            Thread {
                val items = fetch(feedUrls)
                val out = ArrayList<IntelItem>()
                val lock = Object()
                var remaining = items.size
                if (remaining == 0) activity.runOnUiThread { ad.setItems(out) }

                items.forEach { it0 ->
                    TranslateHelper.enToHe(activity, it0.title) { he ->
                        synchronized(lock) {
                            out.add(it0.copy(titleHe = he))
                            remaining--
                            if (remaining == 0) {
                                out.sortByDescending { it.publishedAt }
                                activity.runOnUiThread { ad.setItems(out) }
                            }
                        }
                    }
                }
            }.start()
        }
    }

    // --- Tiny RSS parser (same as IntelFeedActivity) ---
    private fun fetch(urls: List<String>): List<IntelItem> {
        val all = ArrayList<IntelItem>()
        urls.forEach { u ->
            try {
                val xml = java.net.URL(u).readText()
                all.addAll(parse(xml, u))
            } catch (_: Exception) {}
        }
        return all
    }

    private fun parse(xml: String, source: String): List<IntelItem> {
        val out = ArrayList<IntelItem>()
        val f = org.xmlpull.v1.XmlPullParserFactory.newInstance()
        f.isNamespaceAware = true
        val p = f.newPullParser()
        p.setInput(xml.reader())

        var inItem = false
        var title: String? = null
        var link: String? = null

        var event = p.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    when (p.name.lowercase()) {
                        "item", "entry" -> { inItem = true; title = null; link = null }
                        "title" -> if (inItem) title = p.nextText()
                        "link" -> if (inItem) {
                            val href = p.getAttributeValue(null, "href")
                            link = href ?: p.nextText()
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    if (p.name.lowercase() == "item" || p.name.lowercase() == "entry") {
                        inItem = false
                        if (!title.isNullOrBlank()) {
                            out.add(
                                IntelItem(
                                    title = title!!,
                                    link = link,
                                    source = source,
                                    publishedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
            event = p.next()
        }
        return out
    }
}
