package com.pasiflonet.mobile.rss

import android.content.Context

object RssSourceStore {
    private const val PREF = "pasiflonet_prefs"
    private const val KEY = "rss_sources_v1"

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
        val lines = (raw ?: defaultText()).lines()
        return lines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    fun loadText(context: Context): String {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
        return raw ?: defaultText()
    }

    fun saveText(context: Context, text: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, text)
            .apply()
    }

    fun defaultText(): String = """
# הדבק מקורות (שורה לכל מקור)
# אפשרויות:
# 1) RSS רגיל: https://....
# 2) חיפוש X דרך Nitter בתור RSS: q: טקסט חיפוש
# 3) משתמש X דרך Nitter בתור RSS: @username

# --- מקורות רשמיים/חדשות ---
https://www.navy.mil/feeds/press-release.xml
https://www.navy.mil/feeds/top-stories.xml
https://www.defensenews.com/arc/outboundfeeds/rss/?outputType=xml

# --- חיפושים (Nitter RSS) ---
q: US Navy destroyer Red Sea
q: ballistic missile launch
q: airstrike Syria
""".trim()
}
