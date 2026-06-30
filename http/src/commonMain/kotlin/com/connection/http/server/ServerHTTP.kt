package com.connection.http.server


//import io.ktor.server.netty.Netty

import com.connection.http.HttpKMP
import com.connection.http.User
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class ServerHTTP(
    val serverip: String,
    val serverport: Int,
    val servergetEndpoint: String = "sse",
    val serverpostEndpoint: String = "command"
) : HttpKMP(serverip, serverport, servergetEndpoint, serverpostEndpoint) {

    private var vv: MutableList<Flow<String>> = mutableListOf()
    private var users = mutableListOf<User>()

    private var eventsToSendFlow: MutableList<MutableSharedFlow<String>> = mutableListOf()
    fun addEventSharedFlow(eventReceivedFlow: MutableSharedFlow<String>) {
        eventsToSendFlow.add(eventReceivedFlow)
    }

    fun removeEventSharedFlow(eventReceivedFlow: MutableSharedFlow<String>) {
        eventsToSendFlow.add(eventReceivedFlow)
    }


    private fun onPost(msg: String) {
        lastState.update { it.copy(lastData = msg) }
        listeners.forEach { listener ->
            (listener as HttpServerListener).onPost(msg, serverport)
        }
    }


    @OptIn(ExperimentalTime::class)
    private val instance by lazy {

        embeddedServer(CIO, serverport) {
            //      embeddedServer(Netty, portNumber) {

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }

            running = true
            onConnected(true)
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

                post("/$postEndpoint") {
                    try {
                        val command = call.receive<String>()
                        println("Received: ${command}")
                        onPost(command)
                        call.respond(HttpStatusCode.Created, "Command ${command} received")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                    }
                }


                get("/$getEndpoint") {
                    println("ENTRANDO NO SSE")

                    val heartBeatFlow: Flow<String> = flow {
                        while (true) {
                            emit("Running: ${Clock.System.now().epochSeconds}")
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


                get("/$getEndpoint/{id}/{name}") {
                    println("USER ${call.parameters["name"]} TENTANDO ENTRAR NO SSE")
                    var id: Int = 0
                    var name = ""
                    val usersFlow: Flow<String> = flow {
                        try {
                            id = call.parameters["id"]?.toInt() ?: 0
                            if (id == 0) {
                                emit("insira um id valido")
                                return@flow
                            }
                            if (users.any { participant -> participant.id == id }) {
                                emit("$name: ja existe um usuario com este id")
                                return@flow
                            }
                        } catch (ex: Exception) {
                            emit("insira um id valido")
                            return@flow
                        }

                        name = call.parameters["name"] ?: ""
                        if (name == "") {
                            emit("usuario precisa de um nome")
                            return@flow
                        }

                        if (users.any { participant -> participant.name == name }) {
                            emit("$id: ja existe um usuario com este nome")
                            return@flow
                        }


                        users.add(User(name, id))
                        // store.dispatch(SetParticipants(participants))
                        //  users.forEach { participant ->
                        emit("id: ${id}, nome: ${name} acabou de se conectar")
                        // }
                    }

                    val heartBeatFlow: Flow<String> = flow {
                        while (true) {
                            emit(

                                " id: ${id}, nome: ${name}",

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


    fun runBlocking() {
        if (!running) {
            customScope.launch {
                instance.start(wait = true)
//                running = false
//                serverState.value = TiposConexao.Disconnected
            }
        }
    }

    fun stop() {
        customScope.cancel()
        instance.stop()
        running = false

        onConnected(false)
    }
}


expect suspend fun ApplicationCall.streamSse(events: Flow<String>)



