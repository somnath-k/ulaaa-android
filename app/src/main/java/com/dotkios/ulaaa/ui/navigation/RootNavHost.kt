package com.dotkios.ulaaa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dotkios.ulaaa.ui.auth.LoginScreen
import com.dotkios.ulaaa.ui.auth.SignupScreen
import com.dotkios.ulaaa.ui.location.LocationGate
import com.dotkios.ulaaa.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
}

/** Top-level graph: splash → auth (login/signup) → main shell. */
@Composable
fun UlaaaRoot() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onLoggedIn = {
                    nav.navigate(Routes.MAIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                },
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateSignup = { nav.navigate(Routes.SIGNUP) },
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignedUp = {
                    nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBackToLogin = { nav.popBackStack() },
            )
        }

        composable(Routes.MAIN) {
            // Location is mandatory — the shell only renders once permission is granted.
            LocationGate {
                MainShell(
                    onSignOut = {
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.MAIN) { inclusive = true } }
                    },
                )
            }
        }
    }
}
