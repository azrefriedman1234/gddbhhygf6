package com.pasiflonet.mobile.intel

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.atomic.AtomicBoolean

object TranslateHelper {
    @Volatile private var translator: Translator? = null
    private val modelReady = AtomicBoolean(false)
    @Volatile private var downloading = false

    fun ensureModel(ctx: Context, onReady: (Boolean) -> Unit) {
        if (modelReady.get()) {
            onReady(true)
            return
        }

        if (translator == null) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.HEBREW)
                .build()
            translator = Translation.getClient(options)
        }

        if (downloading) {
            // כבר בתהליך הורדה – נחזיר false כרגע, הקריאה הבאה תצליח
            onReady(false)
            return
        }

        downloading = true
        val cond = DownloadConditions.Builder().requireWifi().build()
        translator!!.downloadModelIfNeeded(cond)
            .addOnSuccessListener {
                modelReady.set(true)
                downloading = false
                onReady(true)
            }
            .addOnFailureListener {
                downloading = false
                onReady(false)
            }
    }

    fun enToHe(ctx: Context, text: String, onDone: (String) -> Unit) {
        if (text.isBlank()) {
            onDone(text)
            return
        }

        ensureModel(ctx) { ok ->
            val t = translator
            if (!ok || t == null) {
                onDone(text) // fallback: בלי תרגום
                return@ensureModel
            }
            t.translate(text)
                .addOnSuccessListener { onDone(it) }
                .addOnFailureListener { onDone(text) }
        }
    }

    fun close() {
        try { translator?.close() } catch (_: Exception) {}
        translator = null
        modelReady.set(false)
        downloading = false
    }
}
