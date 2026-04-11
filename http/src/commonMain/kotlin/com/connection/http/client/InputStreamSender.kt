package com.connection.http.client


import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
//import okhttp3.MediaType.Companion.toMediaTypeOrNull

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig.Companion.INFINITE_TIMEOUT_MS
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.Request
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableSharedFlow

class InputStreamSender(
    private val source: String,
    private val endpoint: String,
    private val extraHeaders: Map<String, String>,
    private val scope: CoroutineScope
) {
    private var stop = false
    private var restart = true

    private var isSending = false
    var sending: Boolean
        get() = isSending
        set(value) {
            isSending = value
            if (value) {
                println("Sending Frames")
                //listener?.onStateChanged(ConnectionState.CONNECTED)
            } else {
                println("Not Sending Frames")
                //listener?.onStateChanged(ConnectionState.DISCONNECTED)
            }
        }


    var connectionError = false

    private val iHeaders: Map<String, String> = mapOf(
        Pair("Content-Type", "text/event-stream")
    )

    val mutex = Mutex()
    private var newFrame: String? = null
    //var frameList = mutableListOf<String>()


    suspend fun setFrame(frame: String) {
        mutex.withLock {
            newFrame = frame
            //frameList.add(newFrameString)
        }
    }

    suspend fun getFrame(): String {
        mutex.withLock {
            val ff = newFrame!!
            return ff
        }
    }


    fun stopSend() {
        stop = true
    }


    fun startSend(
        frameRate: Long,
    ) {

        val sendIntervalMs = (1000 / frameRate).toLong()
        scope.launch {
            while (restart) {
                if (sending) {
                    println("Already sending")
                } else {
                    println("Starting send")
                    restart = false
                    // listener?.onStateChanged(source, ConnectionState.CONNECTING)
                    try {
                        connectionError = false
                        // val builder = OkHttpClient().newBuilder()
                        //val okHttpClient = builder.build()


                        val requestBody = object : OutgoingContent.WriteChannelContent() {
                            //override val contentType: ContentType = ContentType.Application.OctetStream// "text/event-stream; charset=utf-8".

                            override suspend fun writeTo(channel: ByteWriteChannel) {

                                // pdfFile.inputStream().copyTo(channel, 1024)


                                while (!stop) {
                                    val data = "data: ${getFrame()}"
                                    println("sender : $data")

                                    val chunkSize = 10
                                    val chunks = data.chunked(chunkSize)
                                    for (chunk in chunks) {
                                        channel.writeStringUtf8(chunk)
                                        channel.flush()
                                    }
                                    channel.writeStringUtf8("\n\n")
                                    channel.flush()
                                    delay(sendIntervalMs)
                                }
                            }
                        }

                        val client = HttpClient()
                        val response: HttpResponse = client.post(endpoint) {
                            timeout {
                                requestTimeoutMillis = INFINITE_TIMEOUT_MS
                            }
                            contentType(ContentType.Application.Json)
                            setBody(requestBody) // Ktor handles serialization
                            headers {
                                iHeaders.forEach { header ->
                                    append(header.key, header.value)
                                }
                                extraHeaders.forEach { header ->
                                    append(header.key, header.value)
                                }
                            }
                        }
                        if (response.status != HttpStatusCode.Accepted) {
                            restart = true
                        }

                        println("Response status: ${response.status}")
                        println("Response body: ${response.bodyAsText()}")
                        client.close()
                        //return response.bodyAsText()

                    } catch (e: Exception) {
                        println("Unable to make request: $e")
                        sending = false
                        connectionError = true
                        //listener?.onStateChanged(source, ConnectionState.CONNECTING)
                        //callback()
                        // sendingJob?.cancel()
                        restart = true
                    }
                }

                delay(1000)
            }
            println("InputStreamSender Stopped")
            // listener?.onStateChanged(source, ConnectionState.DISCONNECTED)
        }
    }


}