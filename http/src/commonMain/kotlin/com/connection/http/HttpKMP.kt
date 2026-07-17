package com.connection.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

open class HttpKMP(
    val ip: String,
    val port: Int,
    val getEndpoint: String,
    val postEndpoint: String,
) {
    protected val lastState = MutableStateFlow<HttpProperties>(HttpProperties())
    val lastStateFlow: SharedFlow<HttpProperties> = lastState


    protected var listeners = mutableListOf<HttpListener>()
    fun addListener(listener: HttpListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: HttpListener) {
        listeners.remove(listener)
    }


    protected fun onConnected(connected: Boolean) {
        lastState.update { it.copy(lastConnectionState = connected) }
        listeners.forEach { listener ->
            listener.onHttpConnected(connected, ip, port)
        }
    }

    protected fun onError(msg: String) {
        lastState.update { it.copy(lastError = msg) }
        listeners.forEach { listener ->
            listener.onHttpError(msg, ip, port)
        }
    }


    var byteArraySocketFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 1
    )



    protected var myJob: Job? = null
    val customScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected var running = false
    protected var errorCount = 0
}