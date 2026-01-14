package com.pasiflonet.mobile.ui

object TranslationBridge {
    /**
     * Best-effort: אם יש אצלך בפרויקט תרגום קיים – אפשר לחבר פה.
     * כרגע: מחזיר אותו טקסט כדי לא לשבור קומפילציה.
     */
    fun translateToHebrew(text: String, onResult: (String) -> Unit) {
        try { onResult(text) } catch (_: Exception) {}
    }
}
