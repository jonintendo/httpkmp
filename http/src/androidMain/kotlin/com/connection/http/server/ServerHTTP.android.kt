package com.connection.http.server


import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respondTextWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch


actual suspend fun ApplicationCall.streamSse(events: Flow<String>) {
    response.cacheControl(CacheControl.NoCache(null))

    respondTextWriter(contentType = ContentType.Text.EventStream) {
        events
            .catch { ws -> println("SAINDO DO SSE ${ws.message}") }
            .collect { event ->
                write("event: ${event}\n")
                //  write("data: $dataStr\n")
                write("\n")
                flush()
            }

    }
}