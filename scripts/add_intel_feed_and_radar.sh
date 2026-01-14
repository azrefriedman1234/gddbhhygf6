#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
APP="$ROOT/app"

JAVA_DIR="$APP/src/main/java/com/pasiflonet/mobile"
UI_DIR="$JAVA_DIR/ui"
INTEL_DIR="$JAVA_DIR/intel"
RES_LAYOUT="$APP/src/main/res/layout"

mkdir -p "$UI_DIR" "$INTEL_DIR" "$RES_LAYOUT"

# --------- Layout: activity_intel_feed.xml ----------
cat > "$RES_LAYOUT/activity_intel_feed.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <EditText
            android:id="@+id/etFeedUrl"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="RSS URL (לדוגמה RSSHub/Nitter/RSS רגיל)"
            android:inputType="textUri"
            android:maxLines="1" />

        <Button
            android:id="@+id/btnSave"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="שמור" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="8dp">

        <Switch
            android:id="@+id/swTranslate"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="תרגום לעברית" />

        <Button
            android:id="@+id/btnRefresh"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="רענן"
            android:layout_marginStart="12dp"/>
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="10dp" />

</LinearLayout>
XML

# --------- Layout: activity_radar.xml ----------
cat > "$RES_LAYOUT/activity_radar.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/rootRadar"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="8dp">

    <com.pasiflonet.mobile.ui.RadarView
        android:id="@+id/radarView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
XML

# --------- Kotlin: models + RSS parser ----------
cat > "$INTEL_DIR/IntelItem.kt" <<'KT'
package com.pasiflonet.mobile.intel

data class IntelItem(
    val id: String,
    val title: String,
    val link: String,
    val pubMillis: Long,
    val source: String,
    var titleHe: String? = null
)
KT

cat > "$INTEL_DIR/RssParser.kt" <<'KT'
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
KT

# --------- Kotlin: Translator helper (ML Kit) ----------
cat > "$INTEL_DIR/TranslateHelper.kt" <<'KT'
package com.pasiflonet.mobile.intel

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TranslateHelper(ctx: Context) {
    private val appCtx = ctx.applicationContext

    private val translator: Translator by lazy {
        val opt = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HEBREW)
            .build()
        Translation.getClient(opt)
    }

    suspend fun ensureModel() {
        suspendCancellableCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) } // לא מפיל את האפליקציה
        }
    }

    suspend fun enToHe(text: String): String? {
        if (text.isBlank()) return null
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    fun close() {
        runCatching { translator.close() }
    }
}
KT

# --------- Kotlin: IntelFeedActivity ----------
cat > "$UI_DIR/IntelFeedActivity.kt" <<'KT'
package com.pasiflonet.mobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.intel.IntelItem
import com.pasiflonet.mobile.intel.RssParser
import com.pasiflonet.mobile.intel.TranslateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IntelFeedActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private lateinit var etUrl: EditText
    private lateinit var swTranslate: Switch
    private lateinit var rv: RecyclerView
    private val items = ArrayList<IntelItem>()
    private val adapter = IntelFeedAdapter(items) { item ->
        val url = item.link
        if (url.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intel_feed)

        etUrl = findViewById(R.id.etFeedUrl)
        swTranslate = findViewById(R.id.swTranslate)
        rv = findViewById(R.id.rv)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val prefs = getSharedPreferences("intel_prefs", Context.MODE_PRIVATE)
        val defUrl = prefs.getString("feed_url", "") ?: ""
        etUrl.setText(defUrl)
        swTranslate.isChecked = prefs.getBoolean("translate", true)

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("feed_url", etUrl.text.toString().trim())
                .putBoolean("translate", swTranslate.isChecked)
                .apply()
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            refresh()
        }

        // רענון ראשון
        refresh()
    }

    private fun refresh() {
        val url = etUrl.text.toString().trim()
        if (url.isBlank()) return

        scope.launch {
            val translateOn = swTranslate.isChecked
            val translator = if (translateOn) TranslateHelper(this@IntelFeedActivity) else null
            if (translator != null) translator.ensureModel()

            val newItems = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body ?: return@use emptyList()
                    body.byteStream().use { ins ->
                        RssParser.parse(ins, source = "X/RSS")
                    }
                }
            }

            items.clear()
            items.addAll(newItems.sortedByDescending { it.pubMillis })

            if (translator != null) {
                // תרגום מהיר רק לכותרות הראשונות כדי לא לתקוע UI
                scope.launch {
                    val top = items.take(25)
                    for (it in top) {
                        if (it.titleHe == null) {
                            it.titleHe = translator.enToHe(it.title)
                            adapter.notifyDataSetChanged()
                        }
                    }
                    translator.close()
                }
            }

            adapter.notifyDataSetChanged()
        }
    }
}
KT

# --------- Kotlin: Adapter ----------
cat > "$UI_DIR/IntelFeedAdapter.kt" <<'KT'
package com.pasiflonet.mobile.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.intel.IntelItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntelFeedAdapter(
    private val items: List<IntelItem>,
    private val onClick: (IntelItem) -> Unit
) : RecyclerView.Adapter<IntelFeedAdapter.VH>() {

    private val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    class VH(val root: View, val title: TextView, val titleHe: TextView, val meta: TextView) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
        }
        val t1 = TextView(ctx).apply { textSize = 16f }
        val t2 = TextView(ctx).apply { textSize = 14f }
        val meta = TextView(ctx).apply { textSize = 12f }
        root.addView(t1)
        root.addView(t2)
        root.addView(meta)
        return VH(root, t1, t2, meta)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = items[pos]
        h.title.text = it.title
        h.titleHe.text = it.titleHe ?: ""
        h.meta.text = "${it.source} • ${if (it.pubMillis > 0) fmt.format(Date(it.pubMillis)) else ""}"
        h.root.setOnClickListener { onClick(it) }
    }
}
KT

# --------- Kotlin: RadarView + RadarActivity ----------
cat > "$UI_DIR/RadarView.kt" <<'KT'
package com.pasiflonet.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // נתונים פשוטים: ערך 0..1 = “כמה חדש”, זווית 0..2π
    data class Blip(val r: Float, val a: Float)

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; style = Paint.Style.STROKE }
    private val blip = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    var blips: List<Blip> = emptyList()
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val rad = min(cx, cy) * 0.92f

        // טבעות
        for (i in 1..4) {
            c.drawCircle(cx, cy, rad * (i / 4f), bg)
        }
        // צלב
        c.drawLine(cx - rad, cy, cx + rad, cy, bg)
        c.drawLine(cx, cy - rad, cx, cy + rad, bg)

        for (b in blips) {
            val rr = rad * (1f - b.r.coerceIn(0f, 1f))
            val x = cx + rr * cos(b.a)
            val y = cy + rr * sin(b.a)
            c.drawCircle(x, y, 10f, blip)
        }
    }
}
KT

cat > "$UI_DIR/RadarActivity.kt" <<'KT'
package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pasiflonet.mobile.R
import kotlin.random.Random

class RadarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radar)

        val v: RadarView = findViewById(R.id.radarView)

        // כרגע דמו ויזואלי (חיבור לנתונים נעשה אחרי שנראה שהפרויקט מתקמפל)
        val blips = (1..40).map {
            RadarView.Blip(r = Random.nextFloat(), a = Random.nextFloat() * (Math.PI.toFloat() * 2f))
        }
        v.blips = blips
    }
}
KT

# --------- Gradle deps ----------
GRADLE="$APP/build.gradle.kts"
if [ -f "$GRADLE" ]; then
  if ! grep -q 'com.google.mlkit:translate' "$GRADLE"; then
    perl -0777 -i -pe '
      if ($_ !~ /com\.google\.mlkit:translate/) {
        s/(dependencies\s*\{)/$1\n    implementation("com.google.mlkit:translate:17.0.3")\n    implementation("androidx.work:work-runtime-ktx:2.9.0")\n    implementation("com.squareup.okhttp3:okhttp:4.12.0")\n/s;
      }
    ' "$GRADLE"
  fi
fi

# --------- Manifest: add activities + INTERNET ----------
MANIFEST="$APP/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST" ]; then
  if ! grep -q 'android.permission.INTERNET' "$MANIFEST"; then
    perl -0777 -i -pe 's/<manifest\b/<manifest\n    xmlns:tools="http:\/\/schemas.android.com\/tools"/s' "$MANIFEST" || true
    perl -0777 -i -pe 's/(<manifest[^>]*>)/$1\n    <uses-permission android:name="android.permission.INTERNET" \/>/s' "$MANIFEST"
  fi

  if ! grep -q 'IntelFeedActivity' "$MANIFEST"; then
    perl -0777 -i -pe 's/(<\/application>)/        <activity android:name="com.pasiflonet.mobile.ui.IntelFeedActivity" android:exported="false" \/>\n        <activity android:name="com.pasiflonet.mobile.ui.RadarActivity" android:exported="false" \/>\n$1/s' "$MANIFEST"
  fi
fi

echo "✅ Added Intel Feed + Radar files. Next: hook menu in MainActivity (best-effort)."

# --------- Best-effort: add overflow menu items in MainActivity ----------
MAIN="$JAVA_DIR/MainActivity.kt"
if [ -f "$MAIN" ]; then
  if ! grep -q 'IntelFeedActivity' "$MAIN"; then
    # imports
    grep -q 'import android.view.Menu' "$MAIN" || perl -0777 -i -pe 's/(import .*?\n)/$1import android.view.Menu\nimport android.view.MenuItem\n/s' "$MAIN"
    grep -q 'import android.content.Intent' "$MAIN" || perl -0777 -i -pe 's/(import .*?\n)/$1import android.content.Intent\n/s' "$MAIN"
    # insert methods before last }
    perl -0777 -i -pe '
      if ($_ !~ /onCreateOptionsMenu/ && $_ =~ /class\s+MainActivity/) {
        s/\n}\s*$/\n\n    override fun onCreateOptionsMenu(menu: Menu): Boolean {\n        menu.add(0, 9001, 0, \"X\\/Intel Feed\")\n        menu.add(0, 9002, 1, \"Radar\")\n        return true\n    }\n\n    override fun onOptionsItemSelected(item: MenuItem): Boolean {\n        return when (item.itemId) {\n            9001 -> { startActivity(Intent(this, IntelFeedActivity::class.java)); true }\n            9002 -> { startActivity(Intent(this, RadarActivity::class.java)); true }\n            else -> super.onOptionsItemSelected(item)\n        }\n    }\n}\n/s;
      }
    ' "$MAIN"
  fi
fi

echo "✅ MainActivity patched (if it matched)."
