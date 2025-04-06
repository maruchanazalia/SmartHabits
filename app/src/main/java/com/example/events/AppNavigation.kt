package com.example.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.events.data.api.AuthService
import com.example.events.data.api.HabitsService
import com.example.events.data.api.UserService
import com.example.events.ui.screens.LoginScreen
import com.example.events.ui.screens.HabitsScreen

@Composable
fun EventsApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val authService = AuthService()
    var accessToken by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                authService = authService,
                onLoginSuccess = { token ->
                    accessToken = token
                    navController.navigate("home")
                }
            )
        }
        composable("home") {
            accessToken?.let { token ->
                val habitsService = HabitsService(token)
                val userService = UserService(token)
                HabitsScreen(
                    habitsService = habitsService,
                    userService = userService,
                    onNavigateToHabits = { navController.navigate("habits") } // Proporciona la lambda aquí
                )
            }
        }
        composable("habits") {
            accessToken?.let { token ->
                val habitsService = HabitsService(token)
                val userService = UserService(token)
                HabitsScreen(habitsService = habitsService, userService = userService)
            }
        }
    }
}