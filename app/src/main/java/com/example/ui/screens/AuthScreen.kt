package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.models.User
import com.example.ui.theme.*

enum class AuthMode {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    initialMode: AuthMode = AuthMode.LOGIN,
    authError: String? = null,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(initialMode) }

    var email by remember { mutableStateOf("developer@efrahope.ai") }
    var password by remember { mutableStateOf("Password123!") }
    var displayName by remember { mutableStateOf("EfraHope Developer") }

    var showResetSentDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .testTag("auth_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Brand Icon & Title
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(ElectricIndigo, CyberCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Auth Lock Icon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = when (mode) {
                        AuthMode.LOGIN -> "Sign in to EfraHope AI Builder"
                        AuthMode.SIGNUP -> "Create your Developer Account"
                        AuthMode.FORGOT_PASSWORD -> "Reset your Password"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Error Banner
                if (!authError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearError,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Input Fields
                if (mode == AuthMode.SIGNUP) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                if (mode != AuthMode.FORGOT_PASSWORD) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )
                }

                // Primary Action Button
                Button(
                    onClick = {
                        when (mode) {
                            AuthMode.LOGIN -> onSignIn(email, password)
                            AuthMode.SIGNUP -> onSignUp(email, password, displayName)
                            AuthMode.FORGOT_PASSWORD -> {
                                onResetPassword(email)
                                showResetSentDialog = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_button")
                ) {
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN -> "Sign In"
                            AuthMode.SIGNUP -> "Create Account"
                            AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mode Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mode == AuthMode.LOGIN) {
                        TextButton(
                            onClick = {
                                mode = AuthMode.FORGOT_PASSWORD
                                onClearError()
                            },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text("Forgot password?", style = MaterialTheme.typography.bodySmall)
                        }

                        TextButton(
                            onClick = {
                                mode = AuthMode.SIGNUP
                                onClearError()
                            },
                            modifier = Modifier.testTag("switch_to_signup_button")
                        ) {
                            Text("Sign Up", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                mode = AuthMode.LOGIN
                                onClearError()
                            },
                            modifier = Modifier.testTag("back_to_login_button")
                        ) {
                            Text("Back to Sign In", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Quick Demo Account Selector (Development Auth Sandbox)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DEVELOPMENT AUTH PROVIDER (QUICK LOGIN):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                email = "developer@efrahope.ai"
                                password = "Password123!"
                                onSignIn(email, password)
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_owner_button")
                        ) {
                            Text("Owner/Dev", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                email = "admin@efrahope.ai"
                                password = "AdminPass123!"
                                onSignIn(email, password)
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_admin_button")
                        ) {
                            Text("Admin", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                email = "guest@efrahope.ai"
                                password = "GuestPass123!"
                                onSignIn(email, password)
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_viewer_button")
                        ) {
                            Text("Viewer", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showResetSentDialog) {
        AlertDialog(
            onDismissRequest = { showResetSentDialog = false },
            title = { Text("Password Reset Link Sent") },
            text = { Text("If an account exists for $email, a reset link has been dispatched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetSentDialog = false
                        mode = AuthMode.LOGIN
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
