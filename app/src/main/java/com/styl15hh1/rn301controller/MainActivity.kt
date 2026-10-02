package com.styl15hh1.rn301controller

import android.os.Bundle
import android.content.Context
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.data.discovery.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.styl15hh1.rn301controller.data.network.YamahaHttpClient
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(AndroidLocales.context(newBase)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtime = ReceiverRuntime.get(applicationContext)
        val settings = runtime.settings
        AndroidLocales.syncPlatform(this, settings)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReceiverViewModel(runtime.repository, SsdpYamahaDiscovery(AndroidSsdpSearch(applicationContext), YamahaReceiverVerifier(YamahaHttpClient(applicationContext))), settings) as T
        }
        setContent {
            val vm: ReceiverViewModel = viewModel(factory = factory)
            LaunchedEffect(vm) { vm.settings.language(settings.state.value.language) }
            val preferences by vm.settings.state.collectAsState()
            val dark = when (preferences.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            ReceiverTheme(dark) {
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                DisposableEffect(lifecycle, vm) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> vm.foreground(true)
                            Lifecycle.Event.ON_STOP -> vm.foreground(false)
                            else -> Unit
                        }
                    }
                    lifecycle.addObserver(observer)
                    vm.foreground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                    onDispose { lifecycle.removeObserver(observer); vm.foreground(false) }
                }
                ReceiverScreen(vm, vm.settings) { AndroidLocales.select(this, vm.settings, it) }
            }
        }
    }
}
