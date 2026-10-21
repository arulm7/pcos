package com.simats.pcos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class ForgotPassword : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var serverErrorMessage by remember { mutableStateOf<String?>(null) }
            
            ForgotPasswordContent(
                serverErrorMessage = serverErrorMessage,
                onSubmitClick = { username, email, newPassword, confirmPassword ->
                    serverErrorMessage = null
                    val url = IpV4Connection.getUrl("forgotpassword.php")
                    val queue = Volley.newRequestQueue(this)
                    val request = object : StringRequest(
                        Request.Method.POST, url,
                        { response ->
                            try {
                                val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                                val jsonObject = JSONObject(cleanResponse)
                                if (jsonObject.getString("status") == "success") {
                                    Toast.makeText(this@ForgotPassword, jsonObject.getString("message"), Toast.LENGTH_LONG).show()
                                    finish()
                                } else {
                                    val errorMsg = jsonObject.getString("message")
                                    serverErrorMessage = errorMsg
                                    Toast.makeText(this@ForgotPassword, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                serverErrorMessage = "Error processing response"
                            }
                        },
                        { error ->
                            serverErrorMessage = "Failed to connect: ${error.message}"
                            Toast.makeText(this@ForgotPassword, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        }) {
                        override fun getParams(): Map<String, String> {
                            val params = HashMap<String, String>()
                            params["username"] = username
                            params["email"] = email
                            params["newpassword"] = newPassword
                            return params
                        }
                    }
                    queue.add(request)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordContent(
    serverErrorMessage: String? = null,
    onSubmitClick: (String, String, String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(serverErrorMessage) {
        if (serverErrorMessage != null) {
            if (serverErrorMessage.contains("username", ignoreCase = true)) {
                usernameError = serverErrorMessage
            } else if (serverErrorMessage.contains("email", ignoreCase = true)) {
                emailError = serverErrorMessage
            } else {
                usernameError = serverErrorMessage
            }
        }
    }

    fun validateAndSubmit() {
        var isValid = true

        if (username.isBlank()) {
            usernameError = "Username cannot be empty"
            isValid = false
        } else {
            usernameError = null
        }

        if (email.isBlank()) {
            emailError = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = null
        }

        if (newPassword.isBlank()) {
            newPasswordError = "New password cannot be empty"
            isValid = false
        } else if (newPassword.length < 8) {
            newPasswordError = "Password must be at least 8 characters"
            isValid = false
        } else if (!newPassword.matches(".*[A-Z].*".toRegex())) {
            newPasswordError = "Password must contain at least one uppercase letter"
            isValid = false
        } else if (!newPassword.matches(".*[a-z].*".toRegex())) {
            newPasswordError = "Password must contain at least one lowercase letter"
            isValid = false
        } else if (!newPassword.matches(".*[!@#\$%^&*()_+=\\[\\]{};':\"\\\\|,.<>/?-].*".toRegex())) {
            newPasswordError = "Password must contain at least one special character"
            isValid = false
        } else {
            newPasswordError = null
        }

        if (confirmPassword != newPassword) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        if (isValid) {
            onSubmitClick(username, email, newPassword, confirmPassword)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Reset Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            
            Text(
                text = "Enter your details to reset your password",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    if (usernameError != null) usernameError = null
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(text = usernameError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(text = emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    if (newPasswordError != null) newPasswordError = null
                    if (confirmPasswordError != null && confirmPassword == it) confirmPasswordError = null
                },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = newPasswordError != null,
                supportingText = {
                    if (newPasswordError != null) {
                        Text(text = newPasswordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPasswordError != null,
                supportingText = {
                    if (confirmPasswordError != null) {
                        Text(text = confirmPasswordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { validateAndSubmit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2))
            ) {
                Text(
                    text = "Reset Password", 
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordContentPreview() {
    ForgotPasswordContent(
        onSubmitClick = { _, _, _, _ -> }
    )
}