package com.connection.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
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


actual fun client(cert: String?):   HttpClient = HttpClient(Apache) {
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