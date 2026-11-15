package com.skinscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skinscan.app.data.ImageUtils
import com.skinscan.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkinScanTheme { App() }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE) }
    var showDisclaimer by remember { mutableStateOf(!prefs.getBoolean("accepted", false)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) vm.cameraUri?.let { vm.setImage(it) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setImage(uri)
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Важно") },
            text = { Text(DISCLAIMER) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("accepted", true).apply()
                    showDisclaimer = false
                }) { Text("Понятно") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Search, null) }, label = { Text("Анализ") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.History, null) }, label = { Text("История") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when {
                tab == 1 -> HistoryScreen(vm) { item ->
                    vm.openHistory(item)
                    tab = 0
                }
                vm.result != null -> ResultScreen(vm)
                else -> HomeScreen(
                    vm,
                    onCamera = {
                        val uri = ImageUtils.newCameraUri(ctx)
                        vm.cameraUri = uri
                        camera.launch(uri)
                    },
                    onGallery = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }
    }
}
