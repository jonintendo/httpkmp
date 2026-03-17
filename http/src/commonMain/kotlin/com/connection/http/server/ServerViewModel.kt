package com.connection.http.server

import androidx.lifecycle.ViewModel
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

    val serverState = MutableStateFlow(TiposConexao.Disconnected)
    val returnGetState = MutableStateFlow("")
    val returnPostState = MutableStateFlow("")


    private var _server: ServerHTTP? = null


    var commandFlow = MutableSharedFlow<TiposComandos>(
        extraBufferCapacity = 1
    )


    private var frameFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 1
    )

    fun onCreate() {
       if( _server == null) {
           _server = ServerHTTP(7733, this)
       }
        _server!!.frameFlow = frameFlow
        onStartCommand()
        println("BackgroundTaskService is ready to conquer!")

    }


    fun onDestroy() {
        if (_server != null) {
            _server!!.stop()
            _server = null
            println("BackgroundTaskService says goodbye!")
        }
    }

    private val scope2 = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun onStartCommand() {
        println("Starting Combatente Service")
        val step = 5.0f
        scope2.launch {
            //  runBlocking {

            val httpJob = launch {
                if (_server?.running == false)
                    _server?.runBlocking()
            }

            val commandJob = launch {
                commandFlow.collect { command ->
                    println("o comando eh : ${command.name}")
                    when (command) {
                        TiposComandos.StartCam -> {
                            println(command.name)
                        }

                        else -> {
                            println("nadaaa")
                        }
                    }
                }
            }

            println("Sending Service Stopped")
            // }
        }

    }


    override fun onPostCommand(command: TiposComandos) {
        val message = "Comando ${command.name} recebido pela httpserver"
        println(message)
        //listener?.onBackEvent(TiposEventos.HTTP, message)
        returnPostState.value = command.name

        commandFlow.tryEmit(command)
    }
}