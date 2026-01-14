package com.pasiflonet.mobile.intel

import android.content.Context

/**
 * Temporary translation helper (no-op) to keep build green.
 * Keeps call-sites stable; later you can plug ML Kit / other engine.
 */
object TranslateHelper {
    fun ensureModel(ctx: Context) { /* no-op */ }
    fun close() { /* no-op */ }

    fun enToHe(text: String): String = text

    fun enToHe(text: String, onResult: (String) -> Unit) {
        onResult(text)
    }
}
