package com.simats.pcos

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class AddPatients : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var serverErrorMessage by remember { mutableStateOf<String?>(null) }
            
            AddPatientsContent(
                serverErrorMessage = serverErrorMessage,
                onRegisterClick = { username, email, password, confirmPassword ->
                    serverErrorMessage = null
                    val url = IpV4Connection.getUrl("signup.php")
                    val queue = Volley.newRequestQueue(this)
                    
                    val postRequest = object : StringRequest(
                        Request.Method.POST, url,
                        { response ->
                            try {
                                val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                                val jsonResponse = JSONObject(cleanResponse)
                                if (jsonResponse.getBoolean("success")) {
                                    Toast.makeText(this@AddPatients, "Patient Added Successfully", Toast.LENGTH_LONG).show()
                                    finish()
                                } else {
                                    val errorMsg = jsonResponse.getString("message")
                                    serverErrorMessage = errorMsg
                                    Toast.makeText(this@AddPatients, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                serverErrorMessage = "Error processing server response"
                                Toast.makeText(this@AddPatients, "Error processing JSON", Toast.LENGTH_LONG).show()
                            }
                        },
                        { error ->
                            Log.d("Error.Response", error.toString())
                            serverErrorMessage = "Failed to connect to the server"
                            Toast.makeText(this@AddPatients, "Failed to connect to the server", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        override fun getParams(): Map<String, String> {
                            val params = HashMap<String, String>()
                            params["username"] = username
                            params["email"] = email
                            params["password"] = password
                            params["confirm_password"] = confirmPassword
                            return params
                        }
                    }
                    queue.add(postRequest)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientsContent(
    serverErrorMessage: String?,
    onRegisterClick: (String, String, String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Sync server error with the username field for "Already exists" errors
    LaunchedEffect(serverErrorMessage) {
        if (serverErrorMessage != null) {
            if (serverErrorMessage.contains("username", ignoreCase = true) || serverErrorMessage.contains("exists", ignoreCase = true)) {
                usernameError = serverErrorMessage
            } else if (serverErrorMessage.contains("email", ignoreCase = true)) {
                emailError = serverErrorMessage
            } else {
                usernameError = serverErrorMessage // Default to username field for general errors
            }
        }
    }

    fun validateAndRegister() {
        var isValid = true

        if (username.isBlank()) {
            usernameError = "Username cannot be empty"
            isValid = false
        } else if (username.length < 3) {
            usernameError = "Username must be at least 3 characters"
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

        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 8) {
            passwordError = "Password must be at least 8 characters"
            isValid = false
        } else if (!password.matches(".*[A-Z].*".toRegex())) {
            passwordError = "Password must contain at least one uppercase letter"
            isValid = false
        } else if (!password.matches(".*[a-z].*".toRegex())) {
            passwordError = "Password must contain at least one lowercase letter"
            isValid = false
        } else if (!password.matches(".*[!@#\$%^&*()_+=\\[\\]{};':\"\\\\|,.<>/?-].*".toRegex())) {
            passwordError = "Password must contain at least one special character"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        if (isValid) {
            onRegisterClick(username, email, password, confirmPassword)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.piv11),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.05f)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Join us to manage your health better",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        if (usernameError != null) usernameError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
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
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address") },
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
                    value = password,
                    onValueChange = { 
                        password = it
                        if (passwordError != null) passwordError = null
                        if (confirmPasswordError != null && confirmPassword == it) confirmPasswordError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) {
                            Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm Password") },
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

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { validateAndRegister() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2))
                ) {
                    Text(
                        text = "Sign Up", 
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}