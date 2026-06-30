package com.connection.http.client

import com.connection.http.HttpListener

interface HttpClientListener : HttpListener {
    fun onEventReceive(event: String, ip: String, port: Int)
}