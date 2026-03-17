package com.connection.http.server

import com.connection.http.SseEvent
import com.connection.http.TiposComandos
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json


class ServerHTTP(
    portNumber: Int,
    listener: HttpServerListener
) {
    var cameraFrameFlow: MutableSharedFlow<String>? = null
    var cameraPositionFlow: MutableSharedFlow<String>? = null
    var droneFrameFlow: MutableSharedFlow<String>? = null
    var droneEventFlow: MutableSharedFlow<String>? = null
    var dronePositionFlow: MutableSharedFlow<String>? = null
    var running = false


    private val instance by lazy {

        embeddedServer(CIO, portNumber) {
            //      embeddedServer(Netty, portNumber) {

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }

            routing {
                get("/") {
                    call.respondText("Hello", ContentType.Text.Plain)
                }

                post("/user") {
                    try {
                        val ff = call.receive<String>()
                        println("Received: ${ff}")

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
                        listener.onPostCommand(command)

                        call.respond(HttpStatusCode.Created, "Command ${command} received")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                    }
                }

//                get("/sse") {
//                    println("ENTRANDO NO SSE")
//
//                    val heartBeatFlow: Flow<SseEvent> = flow {
//                        while (true) {
//                            emit(SseEvent("heartBeat"))
//                            delay(25_000)
//                        }
//                    }
//
//                    val eventFlow: Flow<SseEvent> = flow {
//
//                        frameFlow?.collect { frame ->
//                            emit(
//                                SseEvent(
//                                    event = "frame",
//                                    data = frame
//                                )
//                            )
//                        }
//
//                    }
//
//                    call.streamSse(merge(heartBeatFlow, eventFlow))
//                    //call.streamSse(merge(heartBeatFlow))
//                    println("SAINDO DO SSE")
//                }

                get("/sse") {
                    println( "ENTRANDO NO SSE")

                    val heartBeatFlow: Flow<SseEvent> = flow {
                        while (true) {
                            emit(SseEvent("heartBeat"))
                            delay(25_000)
                        }
                    }

                    val cFrameFlow: Flow<SseEvent> = flow {

                        cameraFrameFlow?.collect { frame ->
                            emit(
                                SseEvent(
                                    event = "cameraFrame",
                                    data = frame
                                )
                            )
                        }

                    }

                    val cPositionFlow: Flow<SseEvent> = flow {

                        cameraPositionFlow?.collect { frame ->
                            emit(
                                SseEvent(
                                    event = "cameraPosition",
                                    data = frame
                                )
                            )
                        }

                    }


                    val dEventFlow: Flow<SseEvent> = flow {
                        droneEventFlow?.collect { event ->
                            emit(
                                SseEvent(
                                    event = "droneEvent",
                                    data = event
                                )
                            )
                        }
                    }

                    val dFrameFlow: Flow<SseEvent> = flow {
                        droneFrameFlow?.collect { frame ->
                            emit(
                                SseEvent(
                                    event = "droneFrame",
                                    data = frame
                                )
                            )
                        }
                    }

                    val dPositionFlow: Flow<SseEvent> = flow {
                        dronePositionFlow?.collect { position ->
                            emit(
                                SseEvent(
                                    event = "dronePosition",
                                    data = position
                                )
                            )
                        }
                    }

                    call.streamSse(merge(heartBeatFlow, cFrameFlow,cPositionFlow, dEventFlow, dFrameFlow,dPositionFlow))
                    //call.streamSse(merge(heartBeatFlow))
                    println( "SAINDO DO SSE")
                }

            }
        }
    }

    fun runBlocking() {
        instance.start(wait = true)
        running = true
    }

    fun stop() {
        instance.stop()
        running = false
    }
}

expect suspend fun ApplicationCall.streamSse(events: Flow<SseEvent>)



