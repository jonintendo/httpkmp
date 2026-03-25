package com.connection.http.client

import com.connection.http.SseEvent


interface HttpClientListener {
    fun onEventReceive(event: SseEvent)
}