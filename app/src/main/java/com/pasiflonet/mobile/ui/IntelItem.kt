package com.pasiflonet.mobile.ui

data class IntelItem(
    val title: String,
    val link: String?,
    val source: String,
    val publishedAt: Long = 0L,
    val titleHe: String? = null
)
