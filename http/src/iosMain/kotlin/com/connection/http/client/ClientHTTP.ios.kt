package com.connection.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import kotlin.time.Duration.Companion.seconds



actual fun client(cert: String?): HttpClient = HttpClient(Darwin) {
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
