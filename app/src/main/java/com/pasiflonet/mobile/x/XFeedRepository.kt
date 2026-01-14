package com.pasiflonet.mobile.x

import java.net.HttpURLConnection
import java.net.URL

object XFeedRepository {

    fun fetchAll(urls: List<String>, timeoutMs: Int = 12000): List<XFeedItem> {
        val out = ArrayList<XFeedItem>()
        for (u in urls) {
            try {
                val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("User-Agent", "Azretr/1.0")
                }
                conn.inputStream.use { ins ->
                    out.addAll(XFeedRss.parse(ins))
                }
            } catch (_: Exception) {}
        }
        // יורד מהחדש לישן
        return out.distinctBy { it.link }
            .sortedByDescending { it.publishedAtMs }
            .take(80)
    }
}
