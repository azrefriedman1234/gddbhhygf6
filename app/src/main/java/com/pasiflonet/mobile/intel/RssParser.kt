package com.pasiflonet.mobile.intel

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object RssParser {
    private val fmts = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    )

    fun parse(input: InputStream, source: String): List<IntelItem> {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(input, null)

        val out = ArrayList<IntelItem>()
        var event = p.eventType
        var inItem = false

        var title: String? = null
        var link: String? = null
        var pub: Long = 0L

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (p.name.lowercase(Locale.US)) {
                        "item", "entry" -> {
                            inItem = true
                            title = null; link = null; pub = 0L
                        }
                        "title" -> if (inItem) title = p.nextText().orEmpty().trim()
                        "link" -> if (inItem) {
                            // RSS: <link>text</link> | Atom: <link href="..."/>
                            val href = p.getAttributeValue(null, "href")
                            link = (href ?: runCatching { p.nextText() }.getOrNull()).orEmpty().trim()
                        }
                        "pubdate", "published", "updated" -> if (inItem) {
                            val t = p.nextText().orEmpty().trim()
                            pub = parseDate(t)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (p.name.equals("item", true) || p.name.equals("entry", true)) {
                        inItem = false
                        val t = title?.takeIf { it.isNotBlank() } ?: continue
                        val l = link?.takeIf { it.isNotBlank() } ?: ""
                        val id = (l.ifBlank { t }) + "|" + pub
                        out.add(IntelItem(id = id, title = t, link = l, pubMillis = pub, source = source))
                    }
                }
            }
            event = p.next()
        }
        return out
    }

    private fun parseDate(s: String): Long {
        for (f in fmts) {
            runCatching { return f.parse(s)?.time ?: 0L }.getOrNull()
        }
        return 0L
    }
}
