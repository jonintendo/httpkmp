package com.connection.http.client

import com.connection.http.HttpListener
import com.connection.http.SseEvent
import com.connection.http.server.HttpServerListener


interface HttpClientListener: HttpListener {
    fun onEventReceive(event: SseEvent)
}