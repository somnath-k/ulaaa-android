package com.dotkios.ulaaa.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.ui.components.AppButton
import kotlinx.coroutines.launch

private val BrandTop = Color(0xFF0D9488)
private val BrandMid = Color(0xFF0A6B62)
private val BrandGlow = Color(0xFFFB6F5C)

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
            .background(Brush.linearGradient(listOf(BrandTop, BrandMid))),
    ) {
        // Soft translucent blobs for a distinctive backdrop.
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-70).dp, y = (-60).dp)
                .background(BrandGlow.copy(alpha = 0.35f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(180.dp)
                .offset(x = 60.dp, y = 40.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(84.dp))
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
            Spacer(Modifier.height(44.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 30.dp),
                ) {
                    Text(
                        text = if (state.verificationId != null) "Verify your number" else "Welcome back",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = when {
                            state.verificationId != null -> "Enter the code we texted you."
                            state.method == AuthMethod.PHONE -> "Sign in with a one-time code."
                            else -> "Sign in to continue planning."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(22.dp))

                    // Hide the method toggle once an OTP is in flight.
                    if (state.verificationId == null) {
                        MethodToggle(
                            method = state.method,
                            enabled = !state.isLoading && !state.otpSending,
                            onSelect = viewModel::useMethod,
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    when {
                        state.method == AuthMethod.PHONE && state.verificationId == null ->
                            PhoneStep(state, viewModel, onSend = {
                                context.findActivity()?.let(viewModel::sendOtp)
                                    ?: viewModel.onGoogleError("Couldn't start verification.")
                            })

                        state.method == AuthMethod.PHONE ->
                            OtpStep(state, viewModel, onResend = {
                                context.findActivity()?.let(viewModel::sendOtp)
                            })

                        else -> EmailStep(state, viewModel)
                    }

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
                        enabled = !state.isLoading && !state.otpSending,
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
private fun PhoneStep(state: AuthUiState, viewModel: AuthViewModel, onSend: () -> Unit) {
    OutlinedTextField(
        value = state.phone,
        onValueChange = viewModel::onPhoneChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Mobile number") },
        prefix = { Text("+91 ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        shape = MaterialTheme.shapes.medium,
    )
    AuthError(state.error)
    Spacer(Modifier.height(22.dp))
    AppButton(
        text = if (state.otpSending) "Sending code…" else "Send OTP",
        onClick = onSend,
        enabled = !state.otpSending,
    )
}

@Composable
private fun OtpStep(state: AuthUiState, viewModel: AuthViewModel, onResend: () -> Unit) {
    OtpBoxes(value = state.otp, onChange = viewModel::onOtpChange)
    AuthError(state.error)
    Spacer(Modifier.height(22.dp))
    AppButton(
        text = if (state.isLoading) "Verifying…" else "Verify & continue",
        onClick = viewModel::verifyOtp,
        enabled = !state.isLoading,
    )
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = viewModel::editPhone, enabled = !state.isLoading) {
            Text("Change number")
        }
        TextButton(onClick = onResend, enabled = !state.isLoading && !state.otpSending) {
            Text(if (state.otpSending) "Resending…" else "Resend code")
        }
    }
}

@Composable
private fun EmailStep(state: AuthUiState, viewModel: AuthViewModel) {
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
    Spacer(Modifier.height(22.dp))
    AppButton(
        text = if (state.isLoading) "Signing in…" else "Sign In",
        onClick = viewModel::login,
        enabled = !state.isLoading,
    )
}

@Composable
private fun MethodToggle(method: AuthMethod, enabled: Boolean, onSelect: (AuthMethod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleTab("Phone", selected = method == AuthMethod.PHONE, enabled = enabled, modifier = Modifier.weight(1f)) {
            onSelect(AuthMethod.PHONE)
        }
        ToggleTab("Email", selected = method == AuthMethod.EMAIL, enabled = enabled, modifier = Modifier.weight(1f)) {
            onSelect(AuthMethod.EMAIL)
        }
    }
}

@Composable
private fun ToggleTab(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OtpBoxes(value: String, onChange: (String) -> Unit, length: Int = 6) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= length && it.all(Char::isDigit)) onChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(length) { index ->
                    val ch = value.getOrNull(index)?.toString() ?: ""
                    val filled = ch.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(
                                BorderStroke(
                                    if (filled) 2.dp else 1.dp,
                                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                ),
                                MaterialTheme.shapes.small,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ch,
                            style = LocalTextStyle.current.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
    )
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

/** Unwraps the Activity from a Compose context — needed to start phone verification. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
