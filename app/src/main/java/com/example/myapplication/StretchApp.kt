package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun StretchApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Login.route
    ) {

        composable(Route.Login.route) {
            LoginScreen(navController)
        }

        composable(Route.Register.route) {
            RegisterScreen(navController)
        }

        composable(Route.Main.route) {
            MainScreen(navController)
        }

        composable(Route.Profile.route) {
            ProfileScreen(navController)
        }


        composable(Route.Chatbot.route) { ChatbotScreen(navController) }
        composable(Route.Record.route) { RecordScreen(navController) }
        composable(Route.Library.route) { LibraryScreen(navController) }
        composable(Route.Calendar.route) { CalendarScreen(navController) }
        composable(Route.Alarm.route) { AlarmScreen(navController) }
        composable(Route.Community.route) { CommunityScreen(navController) }
        composable(Route.Routine.route) { RoutineScreen(navController) }
    }
}