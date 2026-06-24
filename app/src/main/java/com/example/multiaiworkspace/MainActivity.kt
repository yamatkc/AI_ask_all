package com.example.multiaiworkspace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multiaiworkspace.ui.main.MainScreen
import com.example.multiaiworkspace.ui.main.MainViewModel
import com.example.multiaiworkspace.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.webViewController.activity = this

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                            )
                        }
                        composable("settings") {
                            val aiOrder by viewModel.aiOrder.collectAsState()
                            val initialAI by viewModel.initialAI.collectAsState()
                            val enabledProviders by viewModel.enabledProviders.collectAsState()
                            val startupMode by viewModel.startupMode.collectAsState()
                            val coverDisplayCount by viewModel.coverDisplayCount.collectAsState()
                            val expandedDisplayCount by viewModel.expandedDisplayCount.collectAsState()
                            SettingsScreen(
                                aiOrder = aiOrder,
                                initialAI = initialAI,
                                enabledProviders = enabledProviders,
                                startupMode = startupMode,
                                coverDisplayCount = coverDisplayCount,
                                expandedDisplayCount = expandedDisplayCount,
                                onReorder = viewModel::reorderAI,
                                onInitialAIChange = viewModel::saveInitialAI,
                                onEnabledProvidersChange = viewModel::saveEnabledProviders,
                                onStartupModeChange = viewModel::saveStartupMode,
                                onCoverDisplayCountChange = viewModel::saveCoverDisplayCount,
                                onExpandedDisplayCountChange = viewModel::saveExpandedDisplayCount,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.webViewController.activity = this
    }

    override fun onStop() {
        super.onStop()
        // Clear to avoid leaking Activity reference when in background
        viewModel.webViewController.activity = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.webViewController.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.webViewController.flushCookies()
    }
}
