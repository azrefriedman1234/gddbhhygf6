package com.pasiflonet.mobile.intel

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

object TranslateHelper {
    private const val TAG = "TranslateHelper"
    private val exec = Executors.newSingleThreadExecutor()

    @Volatile var apiKey: String = ""
    @Volatile var endpoints: List<String> = listOf(
        "https://libretranslate.de/translate",
        "https://libretranslate.com/translate"
    )

    // ---- API חדש ----
    fun translate(text: String, onResult: (String) -> Unit) =
        translate(ctx = null, text = text, source = "auto", target = "he", onResult = onResult)

    fun translate(text: String, target: String, onResult: (String) -> Unit) =
        translate(ctx = null, text = text, source = "auto", target = target, onResult = onResult)

    fun translateToHebrew(text: String, onResult: (String) -> Unit) =
        translate(ctx = null, text = text, source = "auto", target = "he", onResult = onResult)

    fun translate(
        ctx: Context? = null,
        text: String,
        source: String = "auto",
        target: String = "he",
        onResult: (String) -> Unit
    ) {
        if (text.isBlank()) { onResult(text); return }
        exec.execute {
            val out = runCatching { translateBlocking(text, source, target) }
                .getOrElse { e ->
                    Log.w(TAG, "translate failed: ${e.message}")
                    text
                }
            try { onResult(out) } catch (_: Exception) {}
        }
    }

    @JvmStatic
    fun translateBlocking(text: String, source: String = "auto", target: String = "he"): String {
        val q = text.trim()
        if (q.isEmpty()) return text

        val body = buildString {
            append("q=").append(URLEncoder.encode(q, "UTF-8"))
            append("&source=").append(URLEncoder.encode(source, "UTF-8"))
            append("&target=").append(URLEncoder.encode(target, "UTF-8"))
            append("&format=text")
            if (apiKey.isNotBlank()) append("&api_key=").append(URLEncoder.encode(apiKey, "UTF-8"))
        }.toByteArray(StandardCharsets.UTF_8)

        var lastErr: Exception? = null
        for (ep in endpoints) {
            try {
                val conn = (URL(ep).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 7000
                    readTimeout = 9000
                    doOutput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                }
                conn.outputStream.use { it.write(body) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val resp = stream.bufferedReader().use { it.readText() }

                val json = JSONObject(resp)
                val translated = json.optString("translatedText", "")
                if (translated.isNotBlank()) return translated
            } catch (e: Exception) {
                lastErr = e
            }
        }
        if (lastErr != null) throw lastErr
        return text
    }

    // ---- תאימות לקוד הישן (כדי ש-IntelFeedActivity יתקמפל) ----
    fun ensureModel(ctx: Context) { /* no-op */ }
    fun ensureModel(ctx: Context, onReady: () -> Unit) { onReady() }
    fun ensureModel(ctx: Context, onReady: (Boolean) -> Unit) { onReady(true) }

    fun enToHe(text: String, onResult: (String) -> Unit) {
        translate(ctx = null, text = text, source = "en", target = "he", onResult = onResult)
    }

    fun close() { /* no-op */ }
}
