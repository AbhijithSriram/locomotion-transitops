package com.transitops.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.transitops.driver.data.auth.TokenProvider
import com.transitops.driver.data.sync.SyncWorker
import com.transitops.driver.ui.login.LoginScreen
import com.transitops.driver.ui.login.LoginViewModel
import com.transitops.driver.ui.trip.ActiveTripScreen
import com.transitops.driver.ui.trip.ReportScreen
import com.transitops.driver.ui.trip.TripViewModel
import com.transitops.driver.ui.ai.AiAssistantScreen
import com.transitops.driver.ui.ai.GemmaViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must initialise before any ViewModel reads the token
        TokenProvider.init(applicationContext)

        // Schedule periodic background sync every 15 minutes when online
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "OutboxSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TransitOpsApp()
                }
            }
        }
    }
}

/**
 * Root navigation graph.
 * Routes: "login" → "trip" → "report"
 *
 * The LoginViewModel's init block auto-advances to "trip" if a token
 * is already cached, so returning users never see the login screen.
 */
@Composable
fun TransitOpsApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate("trip") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("trip") {
            val tripViewModel: TripViewModel = viewModel()
            val loginViewModel: LoginViewModel = viewModel()
            ActiveTripScreen(
                viewModel = tripViewModel,
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("trip") { inclusive = true }
                    }
                },
                onNavigateToReport = {
                    navController.navigate("report")
                },
                onNavigateToAssistant = {
                    navController.navigate("assistant")
                }
            )
        }

        composable("assistant") {
            val tripBackStack = navController.getBackStackEntry("trip")
            val tripViewModel: TripViewModel = viewModel(tripBackStack)
            val gemmaViewModel: GemmaViewModel = viewModel()
            AiAssistantScreen(
                gemmaViewModel = gemmaViewModel,
                tripViewModel = tripViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("report") {
            // TripViewModel is scoped to the NavBackStackEntry of "trip" so it
            // shares the same instance — keeping activeTrip state alive.
            val tripBackStack = navController.getBackStackEntry("trip")
            val tripViewModel: TripViewModel = viewModel(tripBackStack)
            ReportScreen(
                viewModel = tripViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
