package com.pasiflonet.mobile.x


data class XItem(
    val text: String = "",
    val link: String? = null,
    val publishedAtMillis: Long = 0L,
    val description: String? = null,
    val published: String? = null
)
