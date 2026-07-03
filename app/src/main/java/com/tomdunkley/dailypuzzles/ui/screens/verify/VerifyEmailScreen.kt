package com.tomdunkley.dailypuzzles.ui.screens.verify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 60

@Composable
fun VerifyEmailScreen(onVerified: () -> Unit) {
    var code by remember { mutableStateOf("") }
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
        topBar = { SectionTopBar(title = "Verify email") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text(
                "Enter the 6-digit code we emailed you to finish setting up your account.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = code,
                onValueChange = { value -> if (value.length <= 6 && value.all(Char::isDigit)) code = value },
                label = { Text("Verification code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && code.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                onClick = {
                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        AuthRepository.verifyEmail(code)
                            .onSuccess { onVerified() }
                            .onFailure { errorMessage = it.toUserMessage("Couldn't verify that code") }
                        isSubmitting = false
                    }
                },
            ) {
                Text("VERIFY")
            }
            TextButton(
                enabled = resendCooldown == 0,
                onClick = {
                    resendCooldown = RESEND_COOLDOWN_SECONDS
                    errorMessage = null
                    coroutineScope.launch {
                        AuthRepository.resendVerification()
                            .onFailure { errorMessage = it.toUserMessage("Couldn't resend code") }
                    }
                },
            ) {
                Text(if (resendCooldown > 0) "RESEND CODE (${resendCooldown}s)" else "RESEND CODE")
            }
            TextButton(onClick = { AuthRepository.signOut() }) {
                Text("SIGN OUT")
            }
        }
    }
}
