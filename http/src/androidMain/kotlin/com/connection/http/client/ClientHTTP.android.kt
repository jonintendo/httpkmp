package com.connection.http.client

import com.connection.http.Header
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds


fun getTrustManager(certInputStream: InputStream): X509TrustManager {
    // 1. Load the certificate from a file (e.g., .crt or .pem)
    //  val certInputStream = FileInputStream("/home/jonathan.oliveira/Downloads/hidra.pem")
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificate = certificateFactory.generateCertificate(certInputStream) as X509Certificate

    // 2. Add the certificate to an in-memory Java KeyStore
    val keyStoreType = KeyStore.getDefaultType()
    val keyStore = KeyStore.getInstance(keyStoreType).apply {
        load(null, null) // Initialize empty keystore
        setCertificateEntry("custom_server_alias", certificate)
    }

    // 3. Initialize a TrustManagerFactory with the KeyStore
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }

    return trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager
}


class ClientHTTPandroid(
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
            try {
                running = true

                onConnected(true)
                val client = HttpClient(OkHttp) {
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

                        config {
//                            val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                                // Restrict allowed connections strictly to TLS 1.3
//                                .tlsVersions(TlsVersion.TLS_1_3)
//                                .build()
//
//                            connectionSpecs(listOf(spec))

                            if (cert != null) {
                                try {
                                    println(cert)
                                    val certInputStream = cert.byteInputStream()
                                    val trustManager = getTrustManager(certInputStream)
                                    val sslContext = SSLContext.getInstance("TLSv1.3").apply {
                                        init(null, arrayOf(trustManager), null)
                                    }
                                    sslSocketFactory(sslContext.socketFactory, trustManager)
                                } catch (e: Exception) {
                                    println(e.message)
                                }
                            }

                        }
                    }
                }

                client.sse(
                    //host = ip, port = port, path = "/$getEndpoint",
                    urlString = "$ip:$port/$getEndpoint",
                    showRetryEvents = true,
                    request = {
                        // Add your custom request headers here
                        optionalHeaders.forEach { optionalHeader ->
                            header(optionalHeader.key, optionalHeader.value)
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
    ClientHTTPandroid(clientip, clientport, clientgetEndpoint, clientpostEndpoint, optionalHeaders)