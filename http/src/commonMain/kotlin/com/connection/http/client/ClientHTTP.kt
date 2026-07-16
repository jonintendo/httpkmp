package com.connection.http.client


//import java.net.URI
//import java.util.concurrent.TimeUnit
//import com.launchdarkly.eventsource.*
import io.ktor.network.tls.*
import com.connection.http.Header
import com.connection.http.HttpKMP
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.Request
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.time.Duration.Companion.seconds


open class ClientHTTP(
    val clientip: String,
    val clientport: Int,
    val clientgetEndpoint: String,
    val clientpostEndpoint: String,
    val optionalHeaders: List<Header>
) : HttpKMP(clientip, clientport, clientgetEndpoint, clientpostEndpoint) {


    protected fun onEventReceive(event: String) {
        lastState.update { it.copy(lastData = event) }
        listeners.forEach { listener ->
            (listener as HttpClientListener).onEventReceive(event, ip, port)
        }
    }

    open fun startSSE(cert: String? = null) {

    }

    fun start() {
        if (running)
            return
        customScope.launch {
            try {
                running = true

                onConnected(true)
                val client = HttpClient(CIO) {

                    install(SSE) {
                        reconnectionTime = 3.seconds
                        maxReconnectionAttempts = 5
                    }

                    engine {

                        https {
                            // Set explicit TLS version (e.g., TLS 1.2)

                            // addProtocol(TLSVersion.TLS12)
                            CIOCipherSuites.SupportedSuites
                        }
                    }
                }


                client.sse(
                    urlString = "$ip:$port/$getEndpoint",
                    showRetryEvents = true,
                    request = {
                        // Add your custom request headers here
                        optionalHeaders.forEach { optionalHeader ->
                            header(optionalHeader.key, optionalHeader.key)
                        }
                    }
                ) {
//                    timeout {
//                        requestTimeoutMillis = INFINITE_TIMEOUT_MS
//                    }
                    incoming.collect { event ->
                        // onEventReceive(SseEvent(event.event!!, event.data))
                        onEventReceive(event.data!!)
                    }
                }
            } catch (e: CancellationException) {
                println("SSE desconectado ${e.message}")
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
//            val response: HttpResponse = client.get("$ip:$port") {
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
                    get(url = "$ip:$port/$getEndpoint", optionalHeaders)
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
                    post(body, url = "$ip:$port/$postEndpoint", optionalHeaders)
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
//            val response: HttpResponse = client.post("$ip:$port/$postendpoint") {
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
            InputStreamSender(source, "$ip:$port/$streamendpoint", mapOf(), customScope)
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
                timeout {
                    requestTimeoutMillis = 10000
                }
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

expect fun getClientHTTP(
    clientip: String,
    clientport: Int,
    clientgetEndpoint: String,
    clientpostEndpoint: String,
    optionalHeaders: List<Header>
): ClientHTTP