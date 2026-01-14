package com.pasiflonet.mobile.x

data class XItem(
    val title: String,
    val text: String,
    val link: String?,
    val publishedAtMillis: Long,
    var heText: String? = null
)
