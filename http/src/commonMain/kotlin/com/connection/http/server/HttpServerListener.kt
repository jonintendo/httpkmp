package com.connection.http.server

import com.connection.http.HttpListener
import com.connection.http.TiposComandos
import com.connection.http.TiposConexao


interface HttpServerListener: HttpListener {
    fun onPostCommand(command: String)
}