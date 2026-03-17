package com.connection.http.server

import com.connection.http.TiposComandos


interface HttpServerListener {
    fun onPostCommand(command: TiposComandos)
}