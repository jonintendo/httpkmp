package com.connection.http.server

import androidx.compose.runtime.mutableStateOf
import com.connection.http.SseEvent
import com.connection.http.TiposComandos
import com.connection.http.TiposConexao
import com.connection.http.TiposEventos
import com.connection.http.User
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE


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
import io.ktor.server.request.receiveChannel
import io.ktor.sse.ServerSentEvent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


class ServerHTTP(
    private val portNumber: Int,
    //private val scope: CoroutineScope,
) {

    private var vv: MutableList<Flow<SseEvent>> = mutableListOf()
    private var running = false
    private var users = mutableListOf<User>()

    var serverStateFlow = MutableStateFlow(TiposConexao.Disconnected)
    private var eventsToSendFlow: MutableList<MutableSharedFlow<SseEvent>> = mutableListOf()
    fun addEventSharedFlow(eventReceivedFlow: MutableSharedFlow<SseEvent>) {
        eventsToSendFlow.add(eventReceivedFlow)
    }

    fun removeEventSharedFlow(eventReceivedFlow: MutableSharedFlow<SseEvent>) {
        eventsToSendFlow.add(eventReceivedFlow)
    }


    private val lastCommandData = MutableStateFlow<String>("")
    val commandFromPostFlow: SharedFlow<String> = lastCommandData
    private var listeners = mutableListOf<HttpServerListener>()
    fun addListener(listener: HttpServerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: HttpServerListener) {
        listeners.remove(listener)
    }

    private fun onPostCommand(command: String) {
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
            serverStateFlow.value = TiposConexao.Connected
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
                        val command = call.receive<String>()
                        println("Received: ${command}")
                        onPostCommand(command)
                        lastCommandData.value = command
                        call.respond(HttpStatusCode.Created, "Command ${command} received")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                    }
                }



                get("/sse") {
                    println("ENTRANDO NO SSE")

                    val heartBeatFlow: Flow<SseEvent> = flow {
                        while (true) {
                            emit(
                                SseEvent(
                                    TiposEventos.HeartBeat.name,
                                    "Running: ${Clock.System.now().epochSeconds}",
                                )
                            )
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


                get("/sse/{id}/{name}") {
                    println("USER ${call.parameters["name"]} TENTANDO ENTRAR NO SSE")
                    var id: Int = 0
                    var name = ""
                    val usersFlow: Flow<SseEvent> = flow {
                        try {
                            id = call.parameters["id"]?.toInt() ?: 0
                            if (id == 0) {
                                emit(
                                    SseEvent(
                                        TiposEventos.USER.name,
                                        "insira um id valido",
                                    )
                                )
                                return@flow
                            }
                            if (users.any { participant -> participant.id == id }) {
                                emit(
                                    SseEvent(
                                        TiposEventos.USER.name,
                                        "$name: ja existe um usuario com este id",
                                    )
                                )
                                return@flow
                            }
                        } catch (ex: Exception) {
                            emit(
                                SseEvent(
                                    TiposEventos.USER.name,
                                    "insira um id valido",
                                )
                            )
                            return@flow
                        }

                        name = call.parameters["name"] ?: ""
                        if (name == "") {
                            emit(
                                SseEvent(
                                    TiposEventos.USER.name,
                                    "usuario precisa de um nome",
                                )
                            )
                            return@flow
                        }

                        if (users.any { participant -> participant.name == name }) {
                            emit(
                                SseEvent(
                                    TiposEventos.USER.name,
                                    "$id: ja existe um usuario com este nome",
                                )
                            )
                            return@flow
                        }


                        users.add(User(name, id))
                        // store.dispatch(SetParticipants(participants))
                        //  users.forEach { participant ->
                        emit(
                            SseEvent(
                                TiposEventos.USER.name,
                                "id: ${id}, nome: ${name} acabou de se conectar",
                            )
                        )
                        // }
                    }

                    val heartBeatFlow: Flow<SseEvent> = flow {
                        while (true) {
                            emit(
                                SseEvent(
                                    TiposEventos.HeartBeat.name,
                                    " id: ${id}, nome: ${name}",
                                )
                            )
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
                        usersFlow, heartBeatFlow
                    )

                    vv.forEach { eventFlow ->
                        gg = merge(gg, eventFlow)
                    }

                    try {
                        call.streamSse(
                            gg
                        )
                    } catch (exception: Exception) {
                        println("problema ${exception.message} com o  ${id} e $name")
                        val problematicUser = users.first { participant -> participant.id == id }
                        users.remove(problematicUser)
                        // store.dispatch(SetParticipants(participants))
                    } finally {
                        println("${id} e $name SAINDO DO SSE")
                    }


                }


                post("/stream") {


                    val channel: ByteReadChannel = call.receiveChannel()

                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(10)
                        println("Received chunk of size: ${packet.remaining}")
                        val text = packet.readText()
                        println("Received frm stream $text")
                    }
                    call.respond(HttpStatusCode.Accepted)
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
        scope2.cancel()
        instance.stop()
        running = false
        serverStateFlow.value = TiposConexao.Disconnected
    }
}

expect suspend fun ApplicationCall.streamSse(events: Flow<SseEvent>)



