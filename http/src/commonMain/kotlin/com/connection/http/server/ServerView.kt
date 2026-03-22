package com.connection.http.server

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun ServerView(viewModel: ServerViewModel) {

    val serverState by viewModel.serverState.collectAsState()

    //val getRequest by viewModel.returnGetState.collectAsState()
    val postRequest by viewModel.returnPostState.collectAsState()


    var address by remember { mutableStateOf("") }
    LaunchedEffect(null) {
        launch {
            address = getAddress()
        }
    }

    Row(modifier = Modifier.padding(top = 50.dp)) {
        Text("Meu IP: $address")
    }

    Text("Status da conexao: $serverState")
    //Text("Get Request: $getRequest")
    Text("Post Request: $postRequest")

    Column() {
        Button(onClick = {
            //startService(serviceIntent)
            viewModel.onCreate()
        }) {
            Text(text = " Start Service")
        }


        Button(onClick = {
            //stopService(serviceIntent)
            viewModel.onDestroy()
        }) {
            Text(text = " Stop Service")
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