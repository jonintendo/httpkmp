package com.connection.http

import kotlinx.serialization.Serializable


@Serializable
data class HttpNotification(var typeNotification: TypeNotification, var msg: String)

@Serializable
enum class TypeNotification() {
    Warning,
    Error,
    Result,
    Info
}

