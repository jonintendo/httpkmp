package com.connection.http

data class SseEvent (
    val event: String,
    val data: String? = null
)