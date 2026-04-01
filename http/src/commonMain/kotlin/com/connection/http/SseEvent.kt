package com.connection.http

data class SseEvent (
    val eventType: String,
    val data: String? = null
)