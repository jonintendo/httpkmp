package com.connection.http.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass



class ServerViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass.isInstance(ServerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServerViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}