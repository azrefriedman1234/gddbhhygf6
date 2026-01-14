package com.pasiflonet.mobile.x

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object XFeedRss {
    private val fmtCandidates = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US)
    )

    private fun parseDateMs(s: String?): Long {
        val t = s?.trim().orEmpty()
        if (t.isEmpty()) return 0L
        for (f in fmtCandidates) {
            try { return f.parse(t)?.time ?: 0L } catch (_: Exception) {}
        }
        return 0L
    }

    fun parse(input: InputStream): List<XFeedItem> {
        val items = ArrayList<XFeedItem>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val p = factory.newPullParser()
        p.setInput(input, "UTF-8")

        var event = p.eventType
        var inItem = false

        var title: String? = null
        var link: String? = null
        var pub: String? = null

        fun flush() {
            val t = title?.trim().orEmpty()
            val l = link?.trim().orEmpty()
            if (t.isNotEmpty() && l.isNotEmpty()) {
                items.add(XFeedItem(t, l, parseDateMs(pub)))
            }
            title = null; link = null; pub = null
        }

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (p.name.lowercase(Locale.ROOT)) {
                        "item", "entry" -> { inItem = true; title = null; link = null; pub = null }
                        "title" -> if (inItem) title = p.nextText()
                        "link" -> if (inItem) {
                            // RSS: <link>text</link>  ATOM: <link href="..."/>
                            val href = p.getAttributeValue(null, "href")
                            link = href ?: p.nextText()
                        }
                        "pubdate", "published", "updated" -> if (inItem) pub = p.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (p.name.lowercase(Locale.ROOT)) {
                        "item", "entry" -> { if (inItem) flush(); inItem = false }
                    }
                }
            }
            event = p.next()
        }

        return items
    }
}
