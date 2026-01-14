package com.pasiflonet.mobile.x

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object XFeedFetcher {
    // חינם: RSS דרך Nitter. אם לא עובד, אפשר להחליף את baseUrl לאינסטנס אחר.
    private const val baseUrl = "https://nitter.net/search/rss?f=tweets&q="

    fun fetch(query: String, maxItems: Int = 30): List<XItem> {
        val url = baseUrl + URLEncoder.encode(query, "UTF-8")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        conn.inputStream.use { input ->
            return parseRss(input).take(maxItems)
        }
    }

    private fun parseRss(input: InputStream): List<XItem> {
        val items = ArrayList<XItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(input, null)

        var event = parser.eventType
        var inItem = false
        var title: String? = null
        var link: String? = null
        var pub: String? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase(Locale.US)) {
                        "item" -> {
                            inItem = true
                            title = null; link = null; pub = null
                        }
                        "title" -> if (inItem) title = parser.nextText()
                        "link" -> if (inItem) link = parser.nextText()
                        "pubdate" -> if (inItem) pub = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && inItem) {
                        val t = title ?: ""
                        val text = t.substringAfter(":", t).trim()
                        items.add(
                            XItem(
                                title = t,
                                text = text,
                                link = link,
                                publishedAtMillis = parseRssDate(pub)
                            )
                        )
                        inItem = false
                    }
                }
            }
            event = parser.next()
        }

        items.sortByDescending { it.publishedAtMillis }
        return items
    }

    private fun parseRssDate(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return try { fmt.parse(s)?.time ?: 0L } catch (_: ParseException) { 0L }
    }
}
