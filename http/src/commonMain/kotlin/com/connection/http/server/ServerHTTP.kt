package com.connection.http.server

import androidx.compose.runtime.mutableStateOf
import com.connection.http.SseEvent
import com.connection.http.TiposComandos
import com.connection.http.TiposConexao
import com.connection.http.User


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
//import io.ktor.server.netty.Netty
import io.ktor.server.engine.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class ServerHTTP(
    private val portNumber: Int,
    private val scope: CoroutineScope,
) {

    private var vv: MutableList<Flow<SseEvent>> = mutableListOf()
    private var running = false

    var serverStateFlow = MutableStateFlow(TiposConexao.Disconnected)
    var eventsToSendFlow: MutableList<MutableSharedFlow<SseEvent>> = mutableListOf()
    fun addEventSharedFlow(eventReceivedFlow: MutableSharedFlow<SseEvent>) {
        eventsToSendFlow.add(eventReceivedFlow)
    }
    fun removeEventSharedFlow(eventReceivedFlow: MutableSharedFlow<SseEvent>) {
        eventsToSendFlow.add(eventReceivedFlow)
    }


    private var listeners = mutableListOf<HttpServerListener>()
    fun addListener(listener: HttpServerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: HttpServerListener) {
        listeners.remove(listener)
    }

    private fun onPostCommand(command: TiposComandos) {
        listeners.forEach { listener ->
            listener.onPostCommand(command)
        }
    }

    @OptIn(ExperimentalTime::class)
    private val instance by lazy {

        embeddedServer(CIO, portNumber) {
            //      embeddedServer(Netty, portNumber) {

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            running = true
            serverStateFlow.value= TiposConexao.Connected
            routing {
                get("/") {
                    call.respondText("Hello", ContentType.Text.Plain)
                }

                post("/user") {
                    try {
                        val ff = call.receive<String>()
                        println("Received: ${ff}")
                        //onPostCommand(TiposComandos.StartCam)
                        // Receive and deserialize JSON to User object
                        val user = call.receive<User>()
                        // Process user (e.g., add to database)
                        println("Received: ${user.name}")

                        call.respond(HttpStatusCode.Created, "User ${user.id} created")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                    }
                }

                post("/command") {
                    try {
                        val command = call.receive<TiposComandos>()
                        println("Received: ${command.name}")
                        onPostCommand(command)

                        call.respond(HttpStatusCode.Created, "Command ${command} received")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                    }
                }



                get("/sse") {
                    println("ENTRANDO NO SSE")

                    val heartBeatFlow: Flow<SseEvent> = flow {
                        while (true) {
                            emit(SseEvent("heartBeat","Running: ${Clock.System.now().epochSeconds}"))
                            delay(25_000)
                        }
                    }

                    eventsToSendFlow.forEach { eventToSendFlow ->
                        vv.add(flow {
                            eventToSendFlow.collect { event ->
                                emit(event)
                            }
                        })
                    }

                    var gg = merge(
                        heartBeatFlow
                    )

                    vv.forEach { eventFlow ->
                        gg = merge(gg, eventFlow)
                    }

                    call.streamSse(
                        gg
                    )

                    println("SAINDO DO SSE")
                }

            }
        }
    }

    private val scope2 = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun runBlocking() {
        if (!running) {
            scope2.launch {
                instance.start(wait = true)
//                running = false
//                serverState.value = TiposConexao.Disconnected
            }
        }
    }

    fun stop() {
        instance.stop()
        running = false
        serverStateFlow.value = TiposConexao.Disconnected
    }
}

expect suspend fun ApplicationCall.streamSse(events: Flow<SseEvent>)



