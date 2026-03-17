package com.connection.http.client

import com.connection.http.TiposConexao


interface HttpListener {
    fun onEventReceive(eventName: String, eventData: String)
    fun onChangeConexionSSE(conexionState: TiposConexao)
    fun onReturnGet(response:String)
    fun onReturnPost(response:String)
}