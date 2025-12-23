package com.example.test_kotlin_compose.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    vm: MyViewModel = viewModel()
) {
    val count by vm.count.collectAsState()
    LaunchedEffect(Unit) {
        vm.loadInitialData()
    }

    // Mỗi khi count thay đổi
    LaunchedEffect(count) {
        println("Count changed: $count")
    }

    // Cleanup (tương đương dispose())
    DisposableEffect(Unit) {
        println("Screen opened")
        onDispose {
            println("Screen closed")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Demo Compose + ViewModel") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Count: $count",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = { vm.increment() }) {
                Text("Increment")
            }
        }
    }
}
