package com.pasiflonet.mobile.intel

data class IntelItem(
    val title: String? = "",
    val description: String? = "",
    val link: String? = "",
    val published: String? = "",
    val text: String? = title,
    val publishedAtMillis: Long = 0L,
    val url: String? = link
)
