package com.connection.http

interface HttpListener {
    fun onConnected(connectionState: TiposConexao)
}