package com.connection.http.client

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.connection.http.client.ClientViewModel


@Composable
fun ClientView(viewModel: ClientViewModel) {


    val clientState by viewModel.clientState.collectAsState()
    val address by viewModel.addressState.collectAsState()
    val getResponse by viewModel.returnGetState.collectAsState()
    val postResponse by viewModel.returnPostState.collectAsState()

    Text("Status da conexao com o servidor $address esta: $clientState")
    Text("Get Response: $getResponse")
    Text("Post Response: $postResponse")


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
    }

}