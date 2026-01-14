package com.pasiflonet.mobile.intel

data class IntelItem(
    val id: String,
    val title: String,
    val link: String,
    val pubMillis: Long,
    val source: String,
    var titleHe: String? = null
)
