package com.tomdunkley.dailypuzzles.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 60

private enum class ForgotPasswordStep { ENTER_EMAIL, ENTER_CODE }

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onResetComplete: () -> Unit) {
    var step by remember { mutableStateOf(ForgotPasswordStep.ENTER_EMAIL) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCooldown by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Reset password",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            when (step) {
                ForgotPasswordStep.ENTER_EMAIL -> {
                    Text(
                        "Enter your account email and we'll send you a reset code.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting && email.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = {
                            isSubmitting = true
                            errorMessage = null
                            coroutineScope.launch {
                                AuthRepository.forgotPassword(email)
                                    .onSuccess {
                                        resendCooldown = RESEND_COOLDOWN_SECONDS
                                        step = ForgotPasswordStep.ENTER_CODE
                                    }
                                    .onFailure { errorMessage = it.toUserMessage("Couldn't send a reset code") }
                                isSubmitting = false
                            }
                        },
                    ) {
                        Text("SEND RESET CODE")
                    }
                }
                ForgotPasswordStep.ENTER_CODE -> {
                    Text(
                        "Enter the 6-digit code we emailed to $email, and choose a new password.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { value -> if (value.length <= 6 && value.all(Char::isDigit)) code = value },
                        label = { Text("Reset code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting && code.length == 6 && newPassword.length >= 8,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = {
                            isSubmitting = true
                            errorMessage = null
                            coroutineScope.launch {
                                AuthRepository.resetPassword(email, code, newPassword)
                                    .onSuccess { onResetComplete() }
                                    .onFailure { errorMessage = it.toUserMessage("Couldn't reset your password") }
                                isSubmitting = false
                            }
                        },
                    ) {
                        Text("RESET PASSWORD")
                    }
                    TextButton(
                        enabled = resendCooldown == 0,
                        onClick = {
                            resendCooldown = RESEND_COOLDOWN_SECONDS
                            errorMessage = null
                            coroutineScope.launch {
                                AuthRepository.resendPasswordReset(email)
                                    .onFailure { errorMessage = it.toUserMessage("Couldn't resend code") }
                            }
                        },
                    ) {
                        Text(if (resendCooldown > 0) "RESEND CODE (${resendCooldown}s)" else "RESEND CODE")
                    }
                }
            }
        }
    }
}
