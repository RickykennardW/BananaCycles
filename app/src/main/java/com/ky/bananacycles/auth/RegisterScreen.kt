package com.ky.bananacycles.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.ky.bananacycles.R

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var fullName by remember {
        mutableStateOf("")
    }
    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var confirmPassword by remember {
        mutableStateOf("")
    }
    var isPasswordVisible by remember {
        mutableStateOf(false)
    }
    var isConfirmPasswordVisible by remember {
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
                RegisterLogoPlaceholder()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create an account to start using BananaCycles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Full Name")
                    },
                    singleLine = true,
                    isError = errorMessage != null
                )

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

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Confirm Password")
                    },
                    singleLine = true,
                    visualTransformation = if (isConfirmPasswordVisible) {
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
                                isConfirmPasswordVisible = !isConfirmPasswordVisible
                            }
                        ) {
                            Text(
                                text = if (isConfirmPasswordVisible) "Hide" else "Show"
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
                        val validationError = validateRegisterInput(
                            fullName = fullName,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword
                        )

                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName.trim())
                                        .build()

                                    auth.currentUser
                                        ?.updateProfile(profileUpdates)
                                        ?.addOnCompleteListener {
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        ?: run {
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                } else {
                                    isLoading = false
                                    errorMessage = task.exception?.localizedMessage
                                        ?: "Registration failed. Please try again later."
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
                        Text("Register")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = onBackToLogin,
                        enabled = !isLoading
                    ) {
                        Text("Login")
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterLogoPlaceholder() {
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

private fun validateRegisterInput(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String
): String? {
    return when {
        fullName.isBlank() -> "Full name is required."
        email.isBlank() -> "Email is required."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Please enter a valid email address."
        password.isBlank() -> "Password is required."
        password.length < 6 -> "Password must be at least 6 characters."
        confirmPassword.isBlank() -> "Confirm password is required."
        password != confirmPassword -> "Password and confirm password must match."
        else -> null
    }
}
