package com.connection.http

data class HttpProperties(
    var lastData: String = "",
    var lastConnectionState: Boolean = false,
    var lastError: String = ""
) {

}
