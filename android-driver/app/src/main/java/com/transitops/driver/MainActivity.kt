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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
 * We define two simple routes: "login" and "trip".
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
            ActiveTripScreen(
                onLogout = {
                    // Navigate back to login
                    navController.navigate("login") {
                        popUpTo("trip") { inclusive = true }
                    }
                }
            )
        }
    }
}
