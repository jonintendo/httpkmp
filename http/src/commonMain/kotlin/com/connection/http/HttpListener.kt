package com.connection.http

interface HttpListener {
    fun onHttpConnected(connectionState: Boolean, ip: String, port: Int)
    fun onHttpError(msg: String, ip: String, port: Int)
}