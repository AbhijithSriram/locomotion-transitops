package com.transitops.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.transitops.driver.ui.login.LoginScreen
import com.transitops.driver.ui.trip.ActiveTripScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transitops.driver.ui.trip.ReportScreen
import com.transitops.driver.ui.trip.TripViewModel

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.transitops.driver.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.transitops.driver.data.auth.TokenProvider.init(applicationContext)

        // Schedule our SyncWorker to run every 15 minutes whenever connected to a network.
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
            // A simple theme wrapper
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
 * Main Navigation host for the Android Driver App.
 * We define three simple routes: "login", "trip", and "report".
 */
@Composable
fun TransitOpsApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate to the trip screen and remove login from the backstack
                    navController.navigate("trip") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("trip") {
            val tripViewModel: TripViewModel = viewModel()
            ActiveTripScreen(
                viewModel = tripViewModel,
                onLogout = {
                    // Navigate back to login
                    navController.navigate("login") {
                        popUpTo("trip") { inclusive = true }
                    }
                },
                onNavigateToReport = {
                    navController.navigate("report")
                }
            )
        }
        composable("report") {
            val tripViewModel: TripViewModel = viewModel()
            ReportScreen(
                tripId = "trip-42",
                vehicleId = "veh-5",
                viewModel = tripViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
