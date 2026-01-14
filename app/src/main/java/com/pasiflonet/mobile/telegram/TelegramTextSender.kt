package com.pasiflonet.mobile.telegram

import android.content.Context
import android.widget.Toast
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

object TelegramTextSender {

    private fun getTargetUsername(ctx: Context): String? {
        val prefs = ctx.getSharedPreferences("pasiflonet_prefs", Context.MODE_PRIVATE)
        val keys = listOf("target_username", "targetUsername", "target_channel", "targetChannel", "target")
        for (k in keys) {
            val v = prefs.getString(k, null)?.trim()
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    /**
     * מנסה להשיג Client קיים אצלך בפרויקט (לפי שדות/מתודות נפוצים).
     * אם לא מצליח - מציג Toast ולא מפיל כלום.
     */
    private fun getTdClientByReflection(): Client? {
        val candidates = listOf(
            "com.pasiflonet.mobile.td.TdLibManager",
            "com.pasiflonet.mobile.TelegramTdLib",
            "com.pasiflonet.mobile.td.TelegramTdLib"
        )
        for (clsName in candidates) {
            try {
                val cls = Class.forName(clsName)

                // Kotlin object => INSTANCE
                val instance = runCatching { cls.getField("INSTANCE").get(null) }.getOrNull()

                // try method getClient()
                runCatching {
                    val m = cls.methods.firstOrNull { it.name == "getClient" && it.parameterTypes.isEmpty() }
                    val c = m?.invoke(instance) as? Client
                    if (c != null) return c
                }

                // try field client
                runCatching {
                    val f = cls.declaredFields.firstOrNull { it.name.equals("client", true) }
                    f?.isAccessible = true
                    val c = f?.get(instance) as? Client
                    if (c != null) return c
                }
            } catch (_: Throwable) {
                // ignore
            }
        }
        return null
    }

    fun sendNewText(ctx: Context, text: String) {
        val msg = text.trim()
        if (msg.isEmpty()) return

        val user = getTargetUsername(ctx)?.trim()?.trimStart('@')
        if (user.isNullOrBlank()) {
            Toast.makeText(ctx, "אין target username בהגדרות", Toast.LENGTH_SHORT).show()
            return
        }

        val client = getTdClientByReflection()
        if (client == null) {
            Toast.makeText(ctx, "לא נמצא TDLib Client פעיל", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) למצוא chat לפי username
        client.send(TdApi.SearchPublicChat(user), Client.ResultHandler { obj ->
            when (obj) {
                is TdApi.Chat -> {
                    val chatId = obj.id
                    val input = TdApi.InputMessageText(
                        TdApi.FormattedText(msg, null),
                        false, true
                    )
                    client.send(
                        TdApi.SendMessage(chatId, 0, null, null, input),
                        Client.ResultHandler {
                            // success/fail לא חובה
                        }
                    )
                }
                is TdApi.Error -> {
                    Toast.makeText(ctx, "שגיאה בחיפוש ערוץ: ${obj.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(ctx, "לא הצלחתי לפתוח ערוץ לפי username", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
