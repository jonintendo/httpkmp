package com.connection.http.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.connection.http.TiposComandos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


@Composable
fun ClientView(viewModel: ClientViewModel, content: @Composable () -> Unit) {


    Column() {
        val clientState by viewModel.clientState.collectAsState()
        val address by viewModel.addressState.collectAsState()
        val getResponse by viewModel.returnGetState
        val postResponse by viewModel.returnPostState
        val eventFromServerState by viewModel.clientEventState.collectAsState()

        Text("Status da conexao com o servidor $address esta: $clientState")
        Text("Get Response: $getResponse")
        Text("Post Response: $postResponse")
        Text("Evento do Servidor: $eventFromServerState")

        Text("So teste: ${viewModel.clientsState.value}")


        OutlinedTextField(
            value = address, //
            onValueChange = { newText ->
                // address = newText //
                viewModel.setAddress(newText)
            },
            label = { Text("Enter server IP address") }, // Optional label
            placeholder = { Text("http://172.28.253.102:7733") } // Optional placeholder
        )

        Row() {
            Button(onClick = {
                viewModel.startClient(address)
            }) {

                Text("Start Client ")
            }

            Button(onClick = {
                viewModel.stopClient()
            }) {
                Text("Stop Client")
            }

            Button(onClick = {
                viewModel.getInfo()
            }) {
                Text("GET!!!!!!!!!!!!")
            }

            Button(onClick = {
                viewModel.postInfo(TiposComandos.CancelReturn)
            }) {
                Text("POST!!!!!!!!!!!!")
            }

            Button(onClick = {
                //stopService(serviceIntent)
                viewModel.addListener()
            }) {
                Text(text = " Add Myself listener")
            }

            Button(onClick = {
                //stopService(serviceIntent)
                viewModel.removeListener()
            }) {
                Text(text = " Remove Myself listener")
            }


        }


    }
//
//    Box(
//        Modifier
//            .fillMaxWidth()
//            .fillMaxHeight()
//    ) {
//        CompositionLocalProvider(
//        ) {
//            content()
//        }
//    }
}