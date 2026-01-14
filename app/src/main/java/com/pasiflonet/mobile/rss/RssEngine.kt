package com.pasiflonet.mobile.rss

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.ui.RadarView
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

data class RssItem(
    val title: String,
    val link: String,
    val publishedMillis: Long,
    val source: String
)

object RssSources {
    private const val PREF_KEY = "rss_urls_multiline"

    // ברירת מחדל טובה (אפשר להחליף מה-UI)
    fun defaultList(): List<String> = listOf(
        // US Navy RSS
        "https://www.navy.mil/DesktopModules/ArticleCS/RSS.ashx?ContentType=11&Site=1067&max=50",
        "https://www.navy.mil/DesktopModules/ArticleCS/RSS.ashx?ContentType=535&Site=1067&max=50",
        // AFCENT RSS
        "https://www.afcent.af.mil/DesktopModules/ArticleCS/RSS.ashx?ContentType=11&Site=681&max=50",
        // DVIDS RSS
        "https://www.dvidshub.net/rss/news"
    )

    fun load(ctx: Context): List<String> {
        val prefs = ctx.getSharedPreferences("pasiflonet_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(PREF_KEY, "")?.trim().orEmpty()
        val list = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        return if (list.isEmpty()) defaultList() else list
    }

    fun save(ctx: Context, urls: List<String>) {
        val prefs = ctx.getSharedPreferences("pasiflonet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY, urls.joinToString("\n")).apply()
    }

    fun openEditor(activity: Activity, onSaved: () -> Unit) {
        val ctx = activity
        val box = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(24)
            minLines = 10
            gravity = Gravity.TOP or Gravity.START
            setText(load(ctx).joinToString("\n"))
        }

        AlertDialog.Builder(ctx)
            .setTitle("מקורות RSS (כל שורה = כתובת)")
            .setView(box)
            .setPositiveButton("שמור") { _, _ ->
                val urls = box.text?.toString().orEmpty()
                    .lines().map { it.trim() }.filter { it.isNotBlank() }
                save(ctx, urls)
                onSaved()
            }
            .setNegativeButton("ביטול", null)
            .show()
    }
}

object TextSanitizer {
    fun stripHtml(s: String): String {
        // ניקוי בסיסי (בלי ספריות)
        return s
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

object HebrewTranslator {
    /**
     * משתמש בתרגום שקיים אצלך עכשיו, בלי תלות קומפילציה.
     * אם MLKit Translate קיים בפרויקט -> Tasks.await(...) דרך Reflection.
     * אחרת מחזיר את המקור (לא מפיל את האפליקציה).
     */
    fun toHebrewOrSame(ctx: Context, text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return t

        // אם זה כבר נראה עברית - לא לתרגם שוב
        if (t.any { it in '\u0590'..'\u05FF' }) return t

        return try {
            // Classes
            val translationCls = Class.forName("com.google.mlkit.nl.translate.Translation")
            val optionsCls = Class.forName("com.google.mlkit.nl.translate.TranslatorOptions")
            val builderCls = Class.forName("com.google.mlkit.nl.translate.TranslatorOptions\$Builder")
            val translateLangCls = Class.forName("com.google.mlkit.nl.translate.TranslateLanguage")
            val tasksCls = Class.forName("com.google.android.gms.tasks.Tasks")

            // TranslateLanguage.ENGLISH / HEBREW
            val en = translateLangCls.getField("ENGLISH").get(null) as String
            val he = translateLangCls.getField("HEBREW").get(null) as String

            // new TranslatorOptions.Builder().setSourceLanguage(en).setTargetLanguage(he).build()
            val builder = builderCls.getConstructor().newInstance()
            builderCls.getMethod("setSourceLanguage", String::class.java).invoke(builder, en)
            builderCls.getMethod("setTargetLanguage", String::class.java).invoke(builder, he)
            val options = builderCls.getMethod("build").invoke(builder)

            // translator = Translation.getClient(options)
            val translator = translationCls.getMethod("getClient", optionsCls).invoke(null, options)

            // downloadModelIfNeeded + await
            val downloadTask = translator.javaClass.getMethod("downloadModelIfNeeded").invoke(translator)
            tasksCls.getMethod("await", Class.forName("com.google.android.gms.tasks.Task")).invoke(null, downloadTask)

            // translate + await => String
            val translateTask = translator.javaClass.getMethod("translate", String::class.java).invoke(translator, t)
            val out = tasksCls.getMethod("await", Class.forName("com.google.android.gms.tasks.Task")).invoke(null, translateTask) as String

            // close if exists
            runCatching { translator.javaClass.getMethod("close").invoke(translator) }
            out
        } catch (_: Throwable) {
            // fallback: לא להפיל כלום
            t
        }
    }
}

class RssAdapter : RecyclerView.Adapter<RssAdapter.VH>() {
    private val items = ArrayList<RssItem>()

    fun setItems(newItems: List<RssItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            setPadding(18)
            textSize = 14f
            // RTL כדי שיראה נורמלי בעברית
            textDirection = TextView.TEXT_DIRECTION_RTL
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        (holder.itemView as TextView).text =
            "• ${it.title}\n${it.source}"
    }

    class VH(v: TextView) : RecyclerView.ViewHolder(v)
}

class RssEngine(
    private val ctx: Context,
    private val rv: RecyclerView,
    private val radar: RadarView?
) {
    private val main = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null

    private val adapter = RssAdapter()

    // שומרים עד 400 (key = link)
    private val cache = LinkedHashMap<String, RssItem>(512, 0.75f, true)
    private val maxKeep = 400
    private val maxShow = 80

    fun start() {
        main.post {
            rv.layoutManager = LinearLayoutManager(ctx)
            rv.adapter = adapter
        }
        scheduleNow()
    }

    fun stop() {
        future?.cancel(true)
        future = null
        exec.shutdownNow()
    }

    fun reschedule() {
        future?.cancel(true)
        scheduleNow()
    }

    private fun scheduleNow() {
        // סריקה תמידית כל דקה
        future = exec.scheduleAtFixedRate({
            runCatching { tick() }
        }, 0, 60, TimeUnit.SECONDS)
    }

    private fun tick() {
        val urls = RssSources.load(ctx)
        val fetched = ArrayList<RssItem>(128)
        for (u in urls) {
            fetched.addAll(fetchOne(u))
        }

        if (fetched.isEmpty()) return

        // מיין לפי זמן (חדש ראשון)
        val sorted = fetched.sortedByDescending { it.publishedMillis }

        // עדכן cache
        for (it in sorted) {
            cache[it.link] = it
        }

        // הגבלה ל-400 (מוחקים הכי ישנים)
        while (cache.size > maxKeep) {
            val firstKey = cache.entries.firstOrNull()?.key ?: break
            cache.remove(firstKey)
        }

        // תצוגה: רק 80 אחרונים
        val toShow = cache.values
            .sortedByDescending { it.publishedMillis }
            .take(maxShow)

        main.post {
            adapter.setItems(toShow)

            // עדכון Radar: בליפים לפי הפריטים האחרונים
            radar?.setBlips(
                toShow.take(18).mapIndexed { idx, item ->
                    RadarView.Blip(
                        label = item.source,
                        // פיזור רנדומלי עקבי לפי hash
                        angleDeg = ((item.link.hashCode() % 360) + 360) % 360f,
                        radius = 0.25f + (0.7f * ((idx % 10) / 10f)),
                        strength = 1f - (idx / 18f)
                    )
                }
            )
        }
    }

    private fun fetchOne(urlStr: String): List<RssItem> {
        val list = ArrayList<RssItem>(32)
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "PasiflonetRSS/1.0")
        }

        conn.inputStream.use { ins ->
            val bytes = BufferedInputStream(ins).readBytes()
            val xml = String(bytes)

            // parse with DOM (פשוט ויציב)
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(bytes.inputStream())
            val items = doc.getElementsByTagName("item")

            val sourceName = sourceFromUrl(urlStr)

            for (i in 0 until items.length) {
                val node = items.item(i)
                val children = node.childNodes
                var title = ""
                var link = ""
                var pub = ""
                var desc = ""

                for (j in 0 until children.length) {
                    val c = children.item(j)
                    val name = c.nodeName ?: continue
                    val text = c.textContent ?: ""
                    when (name.lowercase()) {
                        "title" -> title = text
                        "link" -> link = text
                        "pubdate" -> pub = text
                        "description" -> desc = text
                    }
                }

                title = TextSanitizer.stripHtml(title)
                desc = TextSanitizer.stripHtml(desc)
                link = link.trim()

                if (title.isBlank() || link.isBlank()) continue

                val heTitle = HebrewTranslator.toHebrewOrSame(ctx, title)
                val millis = parseDateMillis(pub)

                list.add(
                    RssItem(
                        title = heTitle.ifBlank { title },
                        link = link,
                        publishedMillis = millis,
                        source = sourceName
                    )
                )
            }
        }

        runCatching { conn.disconnect() }
        return list
    }

    private fun sourceFromUrl(u: String): String {
        return try {
            val host = URL(u).host ?: u
            host.replace("www.", "")
        } catch (_: Throwable) {
            u
        }
    }

    private fun parseDateMillis(pub: String): Long {
        // לא חייב מושלם; אם לא מצליח => עכשיו
        val p = pub.trim()
        if (p.isEmpty()) return System.currentTimeMillis()
        return System.currentTimeMillis()
    }
}
