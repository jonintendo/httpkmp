package com.connection.http

import kotlinx.serialization.Serializable

//@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class User(val name: String, val id: Int)