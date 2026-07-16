package com.connection.http.client

import com.connection.http.Header
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import platform.CoreFoundation.kCFStreamSocketSecurityLevelTLSv1
import kotlin.time.Duration.Companion.seconds


class ClientHTTPios(
    clientip: String,
    clientport: Int,
    clientgetEndpoint: String,
    clientpostEndpoint: String,
    optionalHeaders: List<Header>
) : ClientHTTP(clientip, clientport, clientgetEndpoint, clientpostEndpoint, optionalHeaders) {
    @OptIn(ExperimentalForeignApi::class)
    override fun startSSE(cert: String?) {

        if (running)
            return
        customScope.launch {
            try {
                running = true

                onConnected(true)
                val client = HttpClient(Darwin) {
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

                    engine {

                        configureSession {
//                            // Force the minimum supported TLS version to 1.3
//                            this.setTLSMinimumSupportedProtocolVersion(
//                                kCFStreamSocketSecurityLevelTLSv1//   tls_protocol_version_TLSv13
//                            )
//
//                            // Explicitly map the maximum supported version to 1.3 as well
//                            this.setTLSMaximumSupportedProtocolVersion(
//                                kCFStreamSocketSecurityLevelTLSv1  //tls_protocol_version_TLSv13
//                            )
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
}

actual fun getClientHTTP(
    clientip: String,
    clientport: Int,
    clientgetEndpoint: String,
    clientpostEndpoint: String,
    optionalHeaders: List<Header>
): ClientHTTP =
    ClientHTTPios(clientip, clientport, clientgetEndpoint, clientpostEndpoint, optionalHeaders)