package com.connection.http

data class SseEvent (
    val eventType: TiposEventos,
    val data: String? = null
)