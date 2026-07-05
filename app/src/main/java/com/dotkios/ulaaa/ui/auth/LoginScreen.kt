package com.dotkios.ulaaa.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.ui.components.AppButton
import kotlinx.coroutines.launch

private val BrandTop = Color(0xFF0E7C7B)
private val BrandBottom = Color(0xFF0A5A59)

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateSignup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onLoggedIn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandTop, BrandBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Text(
                text = "Ulaaa",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "Explore Without Limits",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                ) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Sign in to continue planning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))

                    AuthTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email",
                        keyboard = AuthField.EMAIL,
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        keyboard = AuthField.PASSWORD,
                    )

                    AuthError(state.error)
                    Spacer(Modifier.height(24.dp))

                    AppButton(
                        text = if (state.isLoading) "Signing in…" else "Sign In",
                        onClick = viewModel::login,
                        enabled = !state.isLoading,
                    )

                    Spacer(Modifier.height(16.dp))
                    DividerWithText("or")
                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                when (val result = requestGoogleIdToken(context)) {
                                    is GoogleSignInResult.Token -> viewModel.signInWithGoogle(result.idToken)
                                    is GoogleSignInResult.Error -> viewModel.onGoogleError(result.message)
                                    GoogleSignInResult.NotConfigured ->
                                        viewModel.onGoogleError("Google sign-in isn't configured yet.")
                                }
                            }
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Continue with Google")
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateSignup,
                        enabled = !state.isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("New here? Create an account", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DividerWithText(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        HorizontalDivider()
        Surface(color = MaterialTheme.colorScheme.surface) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
