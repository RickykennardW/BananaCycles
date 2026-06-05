package com.ky.bananacycles.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var isPasswordVisible by remember {
        mutableStateOf(false)
    }
    var isLoading by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppLogoPlaceholder()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sign in to continue to BananaCycles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Email")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    isError = errorMessage != null
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Password")
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                isPasswordVisible = !isPasswordVisible
                            }
                        ) {
                            Text(
                                text = if (isPasswordVisible) "Hide" else "Show"
                            )
                        }
                    },
                    isError = errorMessage != null
                )

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val validationError = validateLoginInput(email, password)
                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isLoading = false

                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = task.exception?.localizedMessage
                                        ?: "Login failed. Please check your email and password."
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Login")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = onRegisterClick,
                        enabled = !isLoading
                    ) {
                        Text("Register")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLogoPlaceholder() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.brand_logo),
            contentDescription = "BananaCycles logo",
            modifier = Modifier.size(58.dp)
        )
    }
}

private fun validateLoginInput(
    email: String,
    password: String
): String? {
    return when {
        email.isBlank() -> "Email is required."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Please enter a valid email address."
        password.isBlank() -> "Password is required."
        else -> null
    }
}
