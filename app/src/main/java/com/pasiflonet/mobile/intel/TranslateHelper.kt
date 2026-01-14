package com.pasiflonet.mobile.intel

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslateHelper {
    @Volatile private var translator: Translator? = null

    private fun get(): Translator {
        val existing = translator
        if (existing != null) return existing
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HEBREW)
            .build()
        return Translation.getClient(options).also { translator = it }
    }

    fun ensureModel(onOk: () -> Unit, onErr: (Throwable) -> Unit) {
        val t = get()
        val conditions = DownloadConditions.Builder().build()
        t.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { onErr(it) }
    }

    fun enToHe(text: String, onOk: (String) -> Unit, onErr: (Throwable) -> Unit) {
        val t = get()
        val conditions = DownloadConditions.Builder().build()
        t.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                t.translate(text)
                    .addOnSuccessListener { onOk(it) }
                    .addOnFailureListener { onErr(it) }
            }
            .addOnFailureListener { onErr(it) }
    }

    fun close() {
        translator?.close()
        translator = null
    }
}
