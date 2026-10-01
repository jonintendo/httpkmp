package com.connection.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

open class HttpKMP(
    val ip: String,
    val port: Int,
    val getEndpoint: String,
    val postEndpoint: String,
) {

    protected val lastStatus = MutableStateFlow(HttpStatus())
    val lastStatusFlow = lastStatus.asStateFlow()

    protected val lastNotification = MutableSharedFlow<HttpNotification>()
    val lastNotificationFlow = lastNotification.asSharedFlow()

    protected val lastData = MutableStateFlow<String>("")
    val lastDataFlow = lastData.asStateFlow()

    protected fun onConnected(connectedd: Boolean) {
        lastStatus.update { it.copy(connected = connectedd) }
    }

    protected fun onData(msg: String) {
        lastData.update { msg }
    }

    protected fun onError(msg: String) {
        lastNotification.tryEmit(HttpNotification(TypeNotification.Error, msg))
    }


    protected var myJob: Job? = null
    val customScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected var running = false
    protected var errorCount = 0
}