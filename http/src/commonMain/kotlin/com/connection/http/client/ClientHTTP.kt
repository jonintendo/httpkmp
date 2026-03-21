package com.connection.http.client


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


class ClientHTTP(
    private val url: String,
    private val scope: CoroutineScope,
) {

    private var running = false
    var clientStateFlow = MutableStateFlow(TiposConexao.Disconnected)
    var eventState = mutableStateOf("teste")
    private var listeners = mutableListOf<HttpClientListener>()
    fun addListener(listener: HttpClientListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: HttpClientListener) {
        listeners.remove(listener)
    }

    private fun onEventReceive(eventName: String, eventData: String) {
        listeners.forEach { listener ->
            listener.onEventReceive(eventName, eventData)
        }
    }

//    private fun onChangeConexionSSE(conexionState: TiposConexao) {
//        listeners.forEach { listener ->
//            listener.onChangeConexionSSE(conexionState)
//        }
//    }

//
//    private fun onReturnGet(response: String) {
//        listeners.forEach { listener ->
//            listener.onReturnGet(response)
//        }
//    }
//
//    private fun onReturnPost(response: String) {
//        listeners.forEach { listener ->
//            listener.onReturnPost(response)
//        }
//    }


    private val scope2 = CoroutineScope(Dispatchers.Default + SupervisorJob())


    fun listenCommandsUntilStopped() {
        if (running)
            return
        scope2.launch {
            try {
                running = true
                clientStateFlow.value = TiposConexao.Connected
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
            } finally {
                running = false

                clientStateFlow.value = TiposConexao.Disconnected
            }
        }
    }


    @OptIn(ExperimentalTime::class)
    fun listenCommandsUntilStopped2(eventclient: MutableState<String>) {
        if (running)
            return
        var autoCancelJob: Job? = null
        running = true
        var count = 0
        scope2.launch {
            while (running) {
                eventclient.value = "${count++}"
                eventState.value = "${count++}"
                println("Running: ${Clock.System.now().epochSeconds}")
                delay(500)
            }
        }
    }


    fun stop() {
        if (running) {
            //eventSource.stop()
            scope2.cancel()
        }
    }


    fun get(responseState: MutableState<String>) {
        scope.launch {
            val client = HttpClient(CIO) {
                install(HttpTimeout)
            }
            val response: HttpResponse = client.get("$url") {
                timeout {
                    requestTimeoutMillis = 3000
                }
            }
            //val body: String = response.body()
            responseState.value = response.bodyAsText()

            println("Response status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            client.close()
        }
    }


    fun post(command: TiposComandos, responseState: MutableState<String>) = scope.launch {
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
        //val body: String = response.body()
        responseState.value = response.bodyAsText()
        println("Response status: ${response.status}")
        println("Response body: ${response.bodyAsText()}")
        client.close()
    }


}