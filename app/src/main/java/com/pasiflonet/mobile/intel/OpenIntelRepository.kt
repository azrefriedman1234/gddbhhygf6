package com.pasiflonet.mobile.intel

data class IntelStoryItem(
    val title: String,
    val summary: String = "",
    val source: String = "OSINT",
    val url: String? = null,
    val ts: Long = System.currentTimeMillis()
)

/**
 * Minimal repo that returns items without any paid API.
 * Replace later with RSS/GDELT etc.
 */
object OpenIntelRepository {
    fun loadNow(): List<IntelStoryItem> {
        val now = System.currentTimeMillis()
        return listOf(
            IntelStoryItem("Intel feed ready ✅", "Hook sources later (RSS/GDELT)", "System", null, now),
            IntelStoryItem("Radar widget active", "Blips are demo for now", "System", null, now - 60_000)
        )
    }
}
