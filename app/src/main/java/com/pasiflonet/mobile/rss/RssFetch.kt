package com.pasiflonet.mobile.rss

import android.util.Xml
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RssFetch {

    fun toUrl(sourceLine: String): String? {
        val s = sourceLine.trim()
        if (s.isEmpty() || s.startsWith("#")) return null

        // q: ... -> nitter search rss
        if (s.startsWith("q:", ignoreCase = true)) {
            val q = s.removePrefix("q:").trim()
            if (q.isEmpty()) return null
            val enc = URLEncoder.encode(q, "UTF-8")
            return "https://nitter.net/search/rss?f=tweets&q=$enc"
        }

        // @user -> nitter user rss
        if (s.startsWith("@")) {
            val u = s.removePrefix("@").trim()
            if (u.isEmpty()) return null
            return "https://nitter.net/$u/rss"
        }

        // url
        if (s.startsWith("http://") || s.startsWith("https://")) return s

        return null
    }

    fun fetch(url: String, maxItems: Int = 40): List<RssItem> {
        val xml = download(url) ?: return emptyList()
        return parse(xml, maxItems)
    }

    private fun download(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection)
        conn.connectTimeout = 12_000
        conn.readTimeout = 12_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "PasiflonetRSS/1.0")
        return try {
            conn.inputStream.use { ins ->
                BufferedInputStream(ins).bufferedReader().readText()
            }
        } catch (_: Throwable) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(xmlText: String, maxItems: Int): List<RssItem> {
        val items = ArrayList<RssItem>()
        val parser = Xml.newPullParser()
        parser.setInput(xmlText.reader())

        var inItem = false
        var inEntry = false

        var title: String? = null
        var link: String? = null
        var summary: String? = null
        var published: String? = null

        fun flush() {
            val t = title?.trim().orEmpty()
            if (t.isNotEmpty()) {
                items.add(RssItem(t, link, summary, published))
            }
            title = null; link = null; summary = null; published = null
        }

        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && items.size < maxItems) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                val name = parser.name.lowercase()

                when (name) {
                    "item" -> { inItem = true; title = null; link = null; summary = null; published = null }
                    "entry" -> { inEntry = true; title = null; link = null; summary = null; published = null }

                    "title" -> if (inItem || inEntry) title = parser.nextText()
                    "link" -> if (inItem) {
                        link = parser.nextText()
                    } else if (inEntry) {
                        // Atom: <link href="..."/>
                        val href = (0 until parser.attributeCount)
                            .firstOrNull { parser.getAttributeName(it).equals("href", true) }
                            ?.let { parser.getAttributeValue(it) }
                        if (!href.isNullOrBlank()) link = href
                    }

                    "description" -> if (inItem) summary = parser.nextText()
                    "summary" -> if (inEntry) summary = parser.nextText()

                    "pubdate" -> if (inItem) published = parser.nextText()
                    "published", "updated" -> if (inEntry) published = parser.nextText()
                }
            } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG) {
                val name = parser.name.lowercase()
                if (name == "item" && inItem) { flush(); inItem = false }
                if (name == "entry" && inEntry) { flush(); inEntry = false }
            }
            event = parser.next()
        }

        return items
    }
}
