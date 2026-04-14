package com.example.sisampah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sisampah.model.UserRole
import com.example.sisampah.ui.screens.admin.AdminDashboard
import com.example.sisampah.ui.screens.auth.LoginScreen
import com.example.sisampah.ui.screens.auth.RegisterScreen
import com.example.sisampah.ui.screens.dlh.DLHDashboard
import com.example.sisampah.ui.screens.masyarakat.MasyarakatDashboard
import com.example.sisampah.ui.screens.petugas.PetugasDashboard
import com.example.sisampah.ui.screens.petugas_dokumentasi.PetugasDokumentasiDashboard
import com.example.sisampah.ui.theme.SISampahTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SISampahTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var loggedInUsername by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role, username ->
                    loggedInUsername = username
                    val destination = when (role) {
                        UserRole.MASYARAKAT -> "masyarakat_dashboard"
                        UserRole.PETUGAS_LPS -> "petugas_dashboard"
                        UserRole.ADMIN -> "admin_dashboard"
                        UserRole.DLH -> "dlh_dashboard"
                        UserRole.PETUGAS_DOKUMENTASI_LPS -> "petugas_dokumentasi_dashboard"
                    }
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login")
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("masyarakat_dashboard") {
            MasyarakatDashboard(
                username = loggedInUsername,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("masyarakat_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("petugas_dashboard") {
            PetugasDashboard(
                username = loggedInUsername,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("petugas_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("admin_dashboard") {
            AdminDashboard(
                username = loggedInUsername,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("dlh_dashboard") {
            DLHDashboard(
                username = loggedInUsername,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("dlh_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("petugas_dokumentasi_dashboard") {
            PetugasDokumentasiDashboard(
                username = loggedInUsername,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("petugas_dokumentasi_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
