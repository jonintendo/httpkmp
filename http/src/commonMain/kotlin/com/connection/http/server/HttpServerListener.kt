package com.connection.http.server

import com.connection.http.HttpListener


interface HttpServerListener : HttpListener {
    fun onPost(msg: String, port: Int)
}