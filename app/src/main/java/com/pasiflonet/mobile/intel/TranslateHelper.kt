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
