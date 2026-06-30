package com.connection.http.client


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.connection.http.Header
import com.connection.http.HttpKMP
import com.connection.http.HttpProperties
import com.connection.http.server.ServerHTTP


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
import io.ktor.client.plugins.HttpTimeoutConfig.Companion.INFINITE_TIMEOUT_MS
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
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


class ClientHTTP(
    val clientip: String,
    val clientport: Int,
    val clientgetEndpoint: String,
    val clientpostEndpoint: String
) : HttpKMP(clientip, clientport, clientgetEndpoint, clientpostEndpoint) {


    private fun onEventReceive(event: String) {
        lastState.update { it.copy(lastData = event) }
        listeners.forEach { listener ->
            (listener as HttpClientListener).onEventReceive(event, ip, port)
        }
    }


    fun start() {
        if (running)
            return
        customScope.launch {
            try {
                running = true

                onConnected(true)
                val client = HttpClient {
                    install(HttpTimeout) {
//                // Timeout for the entire request, from start to finish
//                requestTimeoutMillis = 6000
                        //              requestTimeoutMillis = INFINITE_TIMEOUT_MS
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

                client.sse(urlString = "http://$ip:$port/$getEndpoint", showRetryEvents = true) {
//                    timeout {
//                        requestTimeoutMillis = INFINITE_TIMEOUT_MS
//                    }
                    incoming.collect { event ->
                        // onEventReceive(SseEvent(event.event!!, event.data))
                        onEventReceive(event.data!!)
                    }
                }
            } catch (e: CancellationException) {

            } catch (e: Exception) {
                // Handle other exceptions
                // e.printStackTrace()
                println(e.message)
            } finally {
                running = false


                onConnected(false)
            }

        }
    }


    fun stop() {
        if (running) {
            //eventSource.stop()
            customScope.cancel()
        }
    }

//
//    fun get() {
//        scope2.launch {
//            val client = HttpClient(CIO) {
//                install(HttpTimeout)
//            }
//            val response: HttpResponse = client.get("http://$ip:$port") {
//                timeout {
//                    requestTimeoutMillis = 3000
//                }
//            }
//
//            println("Response status: ${response.status}")
//            println("Response body: ${response.bodyAsText()}")
//            client.close()
//        }
//    }

    suspend fun get(): String? {
        try {
            //GET /navegacao/v1/rotas/[id] detalhes
            val deferredResult: Deferred<String> = coroutineScope {
                async {
                    get(url = "http://$ip:$port/$getEndpoint")
                }
            }

            return deferredResult.await()
            //faz o que precisar sincronamente
        } catch (ex: Exception) {
            println(ex.message)
            return ex.message
        }
    }


    suspend fun post(body: String): String? {
        try {
            //GET /navegacao/v1/rotas/[id] detalhes
            val deferredResult: Deferred<String> = coroutineScope {
                async {
                    post(body, url = "http://$ip:$port/$postEndpoint")
                }
            }

            return deferredResult.await()
            //faz o que precisar sincronamente
        } catch (ex: Exception) {
            println(ex.message)
            return ex.message
        }
    }


//    fun post(request: String, postendpoint: String, responseState: MutableState<String>) =
//        scope2.launch {
//            val client = HttpClient(CIO) {
//                install(ContentNegotiation) {
//                    //gson()
//                    json()
//                }
//                install(HttpTimeout)
//            }
//
//
//            val response: HttpResponse = client.post("http://$ip:$port/$postendpoint") {
//                contentType(ContentType.Application.Json)
//                setBody(request) // Ktor handles serialization
//            }
//            //val body: String = response.body()
//            responseState.value = response.bodyAsText()
//            println("Response status: ${response.status}")
//            println("Response body: ${response.bodyAsText()}")
//            client.close()
//        }

    private var inputStreamSender: InputStreamSender? = null

    fun startSendStream(source: String, streamendpoint: String, fps: Long) {
        inputStreamSender =
            InputStreamSender(source, "http://$ip:$port/$streamendpoint", mapOf(), customScope)
        inputStreamSender!!.startSend(fps)
    }

    fun stopSendStream() {
        inputStreamSender?.stopSend()
        inputStreamSender = null
    }

    fun sendStream(source: String, value: String) {
        customScope.launch {
            inputStreamSender?.setFrame(value)
        }
    }

    companion object {

        suspend fun get(url: String, headers: List<Header> = mutableListOf()): String {

            //  scope1.launch {
            val client = HttpClient(CIO) {
                install(HttpTimeout)
            }
            val response: HttpResponse = client.get(url) {
                timeout {
                    requestTimeoutMillis = 3000
                }
                headers {
                    headers.forEach { header ->
                        append(header.key, header.value)
                    }
                }
            }
            //val body: String = response.body()
            //responseState.value = response.bodyAsText()

            println("Response status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            client.close()
            return response.bodyAsText()
            //  }
        }

        suspend fun post(
            body: String,
            url: String,
            headers: List<Header> = mutableListOf<Header>()
        ): String {

            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    //gson()
                    json()
                }
                install(HttpTimeout)
            }

            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body) // Ktor handles serialization
                headers {
                    headers.forEach { header ->
                        append(header.key, header.value)
                    }
                }
            }
            //val body: String = response.body()
            // responseState.value = response.bodyAsText()
            println("Response status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
            client.close()
            return response.bodyAsText()
        }

    }

}
