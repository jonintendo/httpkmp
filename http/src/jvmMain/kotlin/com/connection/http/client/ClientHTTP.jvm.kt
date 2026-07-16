package com.connection.http.client

import com.connection.http.Header
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.util.StringValues
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds


fun createSslContext(certInputStream: InputStream): SSLContext {
    // 1. Load the certificate file (.crt or .pem)
    // val certInputStream = FileInputStream("/home/jonathan.oliveira/Downloads/hidra.pem")

    // 2. Generate the Certificate object
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificate = certificateFactory.generateCertificate(certInputStream)

    // 3. Create a KeyStore and place the certificate inside it
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null) // Initialize an empty keystore
        setCertificateEntry("custom_ca", certificate)
    }

    // 4. Initialize TrustManagerFactory with the KeyStore
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }

    // 5. Build and return the SSLContext
    return SSLContext.getInstance("TLS").apply {
        init(null, trustManagerFactory.trustManagers, null)
    }
}



class ClientHTTPjvm(
    clientip: String,
    clientport: Int,
    clientgetEndpoint: String,
    clientpostEndpoint: String,
    optionalHeaders: List<Header>
) : ClientHTTP(clientip, clientport, clientgetEndpoint, clientpostEndpoint, optionalHeaders) {


    override fun startSSE(cert: String?) {
        if (running)
            return


        customScope.launch {

            val client = HttpClient(Apache) {
                install(HttpTimeout) {
//                // Timeout for the entire request, from start to finish
//                requestTimeoutMillis = 6000
                    //              requestTimeoutMillis = INFINITE_TIMEOUT_MS
//                // Timeout for establishing the connection
//                connectTimeoutMillis = 1000
//                // Maximum time between two data packets (useful for SSE streams)
                    socketTimeoutMillis = 30_000 // 10 minutes, for example
                }
                install(SSE) {
                    reconnectionTime = 3.seconds
                    maxReconnectionAttempts = 5
                }

                engine {
                    if (cert != null) {
                        try {
                            val certInputStream =  cert.byteInputStream()
                            //  val certInputStream = FileInputStream("/home/jonathan.oliveira/Downloads/hidra.pem")
                            sslContext = createSslContext(certInputStream)
                        } catch (e: Exception) {
                            println(e.message)
                        }
                    }
//                    protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
//                    config {
//                        sslContext(createSslContext())
//
////                            // Enforce explicit TLS version via the system's SSLContext
////                            sslContext(SSLContext.getInstance("TLSv1.3").apply {
////                                init(null, null, null)
////
////                            })
//                    }
                }
            }
            running = true
            onConnected(true)
            try {
                client.sse(
                    //host = ip, port = port, path = "/$getEndpoint",
                    urlString = "$ip:$port/$getEndpoint",
                    showRetryEvents = true,
                    request = {
                        optionalHeaders.forEach {
                            header(it.key, it.value)
                        }
                    }
                ) {

                    incoming.collect { event ->
                        // onEventReceive(SseEvent(event.event!!, event.data))
                        onEventReceive(event.data.toString())
                    }
                }
            } catch (e: CancellationException) {
                println("SSE desconectado ${e.message}")
            } catch (e: SSEClientException) {
                println(e.response)
                println(e.message)
                //println(e.)

            } catch (e: Exception) {
                // Handle other exceptions
                // e.printStackTrace()
                //println(e.message)
                println(e.toString())
            } finally {
                client.close()
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
    ClientHTTPjvm(clientip, clientport, clientgetEndpoint, clientpostEndpoint, optionalHeaders)