package com.connection.http.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connection.http.TiposComandos
import com.connection.http.TiposConexao
import kotlinx.coroutines.flow.MutableStateFlow

class ClientViewModel : ViewModel(), HttpListener {


    val clientEventState = MutableStateFlow("")

    val clientState = MutableStateFlow(TiposConexao.Disconnected)

    val addressState = MutableStateFlow("http://172.28.253.102:7733")
    val returnGetState = MutableStateFlow("")
    val returnPostState = MutableStateFlow("")

    private var _client: ClientHTTP? = null

    init {
        //startClient()
    }

    override fun onCleared() {
        super.onCleared()
        // stopClient()
    }

    fun setAddress(address: String) {
        addressState.value = address
    }

    fun startClient(address: String) {
        if (_client != null) {
            println("client already connected")
            return
        }
        _client = ClientHTTP(
            url = address,
            scope = viewModelScope,
        )
        _client?.listenCommandsUntilStopped()
        _client?.addListener(this)

    }

    fun addListeners(listener: HttpListener) {
        _client?.addListener(listener)
    }

    fun removeListeners(listener: HttpListener) {
        _client?.removeListener(listener)
    }

    fun stopClient() {
        _client?.stop()
        _client = null
    }

    fun postInfo(command: TiposComandos) {
        _client?.post(command)
    }

    fun getInfo() {
        _client?.get()
    }

    override fun onEventReceive(eventName: String, eventData: String) {
        clientEventState.value = eventData
        when (eventName) {
            "heartBeat" -> {
                println("eventName $eventName,eventData: $eventData")
            }
        }
    }

    override fun onChangeConexionSSE(conexionState: TiposConexao) {
        clientState.value = conexionState
        println("Connexion State: ${conexionState}")
    }

    override fun onReturnGet(response: String) {
        returnGetState.value = response

    }

    override fun onReturnPost(response: String) {
        returnPostState.value = response
    }

}