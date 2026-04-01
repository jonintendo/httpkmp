package com.connection.http.server

import com.connection.http.TiposComandos
import com.connection.http.TiposConexao


interface HttpServerListener {
    fun onPostCommand(command: String)
}