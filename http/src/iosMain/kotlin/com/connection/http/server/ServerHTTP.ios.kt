package com.connection.http.server

import com.connection.http.SseEvent
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.cacheControl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch


actual suspend fun ApplicationCall.streamSse(events: Flow<SseEvent>) {
    response.cacheControl(CacheControl.NoCache(null))

}