package com.connection.http.server

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connection.http.SseEvent
import com.connection.http.TiposComandos
import com.connection.http.TiposConexao

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ServerViewModel : ViewModel(),
    HttpServerListener {


    val serverEventState = MutableStateFlow("")

    var serverState = MutableStateFlow(TiposConexao.Disconnected)

    //var serverState = mutableStateOf(TiposConexao.Disconnected)
    val returnPostState = MutableStateFlow("nothing yet")


    private var _server: ServerHTTP? = null
    var eventsToSendFlow: MutableList<MutableSharedFlow<SseEvent>> = mutableListOf()


    fun onCreate() {
        if (_server == null) {
            _server = ServerHTTP(
                7733,
              //  viewModelScope
            )
            println(eventsToSendFlow.count())
            eventsToSendFlow.forEach { event ->
                _server?.addEventSharedFlow(event)
            }

            _server?.runBlocking()
            _server!!.serverStateFlow = serverState
            // _server!!.serverState = serverState
            println("BackgroundTaskService is ready to conquer!")
        }
    }

    fun addListener(listener:HttpServerListener) {
        _server?.addListener(listener)
    }

    fun removeListener(listener: HttpServerListener) {
        _server?.removeListener(listener)
    }

    fun onDestroy() {
        if (_server != null) {
            _server!!.stop()
            _server = null
            println("BackgroundTaskService says goodbye!")
        }
    }


    override fun onPostCommand(command: String) {
        val message = "Comando ${command} recebido pela httpserver"
        println(message)
        returnPostState.value = command
    }

}