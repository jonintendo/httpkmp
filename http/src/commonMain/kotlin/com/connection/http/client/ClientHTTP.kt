package com.connection.http.client


import com.connection.http.TiposComandos
import com.connection.http.TiposConexao

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.ContentType
import io.ktor.http.contentType


//import java.net.URI
//import java.util.concurrent.TimeUnit
//import com.launchdarkly.eventsource.*
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlin.time.Duration.Companion.seconds


class ClientHTTP(
    private val url: String,
    private val scope: CoroutineScope,
) {

    private var running = false
    private var stopping = false
    private var connected = false
    private var sampling = false
    private var listeners = mutableListOf<HttpListener>()

    fun addListener(listener: HttpListener) {
        listeners.add(listener)
    }


    fun removeListener(listener: HttpListener) {
        listeners.remove(listener)
    }

    private fun onEventReceive(eventName: String, eventData: String) {
        listeners.forEach { listener ->
            listener.onEventReceive(eventName, eventData)
        }
    }

    private fun onChangeConexionSSE(conexionState: TiposConexao) {
        listeners.forEach { listener ->
            listener.onChangeConexionSSE(conexionState)
        }
    }

    private fun onReturnGet(response: String) {
        listeners.forEach { listener ->
            listener.onReturnGet(response)
        }
    }

    private fun onReturnPost(response: String) {
        listeners.forEach { listener ->
            listener.onReturnPost(response)
        }
    }


    private val scope2 = CoroutineScope(Dispatchers.Default + SupervisorJob())


    fun listenCommandsUntilStopped() {

        if (running)
            return

        running = true


        val client = HttpClient {
            install(HttpTimeout) {
//                // Timeout for the entire request, from start to finish
//                requestTimeoutMillis = 6000
//                // Timeout for establishing the connection
//                connectTimeoutMillis = 1000
//                // Maximum time between two data packets (useful for SSE streams)
//                socketTimeoutMillis = 30_000 // 10 minutes, for example
            }

            install(SSE) {
                reconnectionTime = 3.seconds
                maxReconnectionAttempts = 5
            }
        }

        scope2.launch {
            try {
                onChangeConexionSSE(TiposConexao.Connected)
                client.sse(urlString = "$url/sse", showRetryEvents = true) {
//                    timeout {
//                        requestTimeoutMillis = INFINITE_TIMEOUT_MS
//                    }
                    incoming.collect { event ->
                        onEventReceive(event.event!!, event.data!!)
                    }
                }
            } catch (e: CancellationException) {

            } catch (e: Exception) {
                // Handle other exceptions
               // e.printStackTrace()
                println(e.message)
            }finally {
                running = false
                onChangeConexionSSE(TiposConexao.Disconnected)
            }
        }
    }


//
//    private val eventSource = EventSource.Builder(
//        ConnectStrategy
//            .http(URI("$url/sse"))
//            .readTimeout(30L, TimeUnit.SECONDS)
//    )
//        .expectFields()
//        .errorStrategy(ErrorStrategy.alwaysContinue())
//        .build()
//
//
//    fun listenCommandsUntilStopped2() {
//        if (running)
//            return
//        var autoCancelJob: Job? = null
//        running = true
//        scope2.launch {
//            while (running) {
//                when (val event = eventSource.readAnyEvent()) {
//                    is MessageEvent -> {
//                        onEventReceive(event.eventName, event.data)
//                    }
//
//                    is FaultEvent -> {
//                        if (stopping) {
//                            if (sampling) {
//
//                                autoCancelJob?.cancel()
//                                autoCancelJob = null
//                            }
//
//                            stopping = false
//                            connected = false
//                            running = false
//
//                            onChangeConexionSSE(TiposConexao.Reconnecting)
//                        } else if (connected) {
//
//                            connected = false
//                            onChangeConexionSSE(TiposConexao.Disconnected)
//                        }
//
//                    }
//
//                    is StreamEvent -> {
//                        if (!connected) {
//                            connected = true
//                        }
//                        onChangeConexionSSE(TiposConexao.Connected)
//                    }
//
//                    else -> {
//                        println("Event: ${event}")
//                    }
//                }
//                println("Running: ${Clock.System.now().epochSeconds}")
//                // delay(500)
//            }
//        }
//    }


    fun stop() {
        if (running) {
            //eventSource.stop()
            scope2.cancel()
        }
    }


    fun get() {
        scope.launch {
            val client = HttpClient(CIO) {
                install(HttpTimeout)
            }
            val response: HttpResponse = client.get("$url") {
                timeout {
                    requestTimeoutMillis = 3000
                }
            }
            val body: String = response.body()
            onReturnGet(body)
            println("Response status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            client.close()
        }
    }


    fun post(command: TiposComandos) = scope.launch {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                //gson()
                json()
            }
            install(HttpTimeout)
        }


        val response: HttpResponse = client.post("$url/command") {
            contentType(ContentType.Application.Json)
            setBody(command) // Ktor handles serialization
        }
        val body: String = response.body()
        onReturnPost(body)
        println("Response status: ${response.status}")
        println("Response body: ${response.bodyAsText()}")
        client.close()
    }


    fun post() = scope.launch {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout)
        }

        val user = User("John Doe", 123)
        val response: HttpResponse = client.post("$url/user") {
            contentType(ContentType.Application.Json)
            setBody(user) // Ktor handles serialization
        }
        val body: String = response.body()
        onReturnPost(body)
        println("Response status: ${response.status}")
        println("Response body: ${response.bodyAsText()}")
        client.close()
    }


}