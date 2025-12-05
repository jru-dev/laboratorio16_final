package com.tecsup.galvanguerrero.eventplanner_azanero.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Timestamp
import com.tecsup.galvanguerrero.eventplanner_azanero.data.Event
import com.tecsup.galvanguerrero.eventplanner_azanero.ui.screens.*
import com.tecsup.galvanguerrero.eventplanner_azanero.ui.viewmodel.AuthViewModel
import com.tecsup.galvanguerrero.eventplanner_azanero.ui.viewmodel.EventViewModel
import java.util.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object EventList : Screen("event_list")
    object CreateEvent : Screen("create_event")
    object EditEvent : Screen("edit_event/{eventId}/{title}/{date}/{description}") {
        fun createRoute(event: Event): String {
            return "edit_event/${event.id}/${event.title}/${event.date.seconds}/${event.description}"
        }
    }
}

@Composable
fun NavigationGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = if (currentUser != null) {
        Screen.EventList.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.resetAuthState()
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.EventList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    authViewModel.resetAuthState()
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.EventList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EventList.route) {
            // Crear un nuevo ViewModel cada vez que se entra a esta pantalla
            // y hay un usuario autenticado
            val eventViewModel: EventViewModel = viewModel(
                key = "eventViewModel_${currentUser?.uid ?: "none"}"
            )

            // Reiniciar el listener cuando cambia el usuario
            LaunchedEffect(currentUser?.uid) {
                if (currentUser != null) {
                    eventViewModel.reinitializeListener()
                }
            }

            EventListScreen(
                eventViewModel = eventViewModel,
                authViewModel = authViewModel,
                onNavigateToCreate = {
                    eventViewModel.resetOperationState()
                    navController.navigate(Screen.CreateEvent.route)
                },
                onNavigateToEdit = { event ->
                    eventViewModel.resetOperationState()
                    navController.navigate(Screen.EditEvent.createRoute(event))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateEvent.route) {
            val eventViewModel: EventViewModel = viewModel(
                key = "eventViewModel_${currentUser?.uid ?: "none"}"
            )

            CreateEventScreen(
                eventViewModel = eventViewModel,
                onNavigateBack = {
                    eventViewModel.resetOperationState()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("date") { type = NavType.LongType },
                navArgument("description") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventViewModel: EventViewModel = viewModel(
                key = "eventViewModel_${currentUser?.uid ?: "none"}"
            )

            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val dateSeconds = backStackEntry.arguments?.getLong("date") ?: 0L
            val description = backStackEntry.arguments?.getString("description") ?: ""

            val event = Event(
                id = eventId,
                title = title,
                date = Timestamp(Date(dateSeconds * 1000)),
                description = description
            )

            EditEventScreen(
                event = event,
                eventViewModel = eventViewModel,
                onNavigateBack = {
                    eventViewModel.resetOperationState()
                    navController.popBackStack()
                }
            )
        }
    }
}