package com.simats.pcos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

class PatientLogin : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            PatientLoginContent(
                onLoginClick = { username, password ->
                    if (username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Username and password cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        login(username, password)
                    }
                },
                onForgotPasswordClick = {
                    startActivity(Intent(this, ForgotPassword::class.java))
                },
                onCreateAccountClick = {
                    startActivity(Intent(this, AddPatients::class.java))
                }
            )
        }
    }

    private fun login(username: String, password: String) {
        val url = IpV4Connection.getUrl("login.php")
        val queue = Volley.newRequestQueue(this)

        val postRequest = object : StringRequest(
            Request.Method.POST, url,
            { response -> handleResponse(response, username) },
            { error -> Toast.makeText(applicationContext, "Login failed: $error", Toast.LENGTH_LONG).show() }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["username"] = username
                params["password"] = password
                return params
            }
        }
        queue.add(postRequest)
    }

    private fun handleResponse(response: String, username: String) {
        Log.e("TAG", "response $response")
        try {
            val cleanedResponse = IpV4Connection.cleanJsonResponse(response)
            val jsonResponse = JSONObject(cleanedResponse)
            val success = jsonResponse.getBoolean("success")
            val message = jsonResponse.getString("message")

            val prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val isLocallyCompleted = prefs.getBoolean("details_completed_$username", false)

            if (success) {
                SessionManager.saveSession(this, SessionManager.ROLE_PATIENT, username)
                val isDetailsComplete = message.contains("user details are complete", ignoreCase = true) || isLocallyCompleted

                if (isDetailsComplete) {
                    prefs.edit()
                        .putString("logged_in_patient", username)
                        .putBoolean("details_completed_$username", true)
                        .apply()
                    val intent = Intent(this, PatientHome::class.java).apply {
                        putExtra("username", username)
                        putExtra("patient_name", username)
                    }
                    startActivity(intent)
                    finish()
                } else if (message.contains("complete your personal details", ignoreCase = true)) {
                    val intent = Intent(this, EnterDetails::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    prefs.edit().putString("logged_in_patient", username).apply()
                    val intent = Intent(this, PatientHome::class.java).apply {
                        putExtra("username", username)
                        putExtra("patient_name", username)
                    }
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(applicationContext, "Login failed: $message", Toast.LENGTH_LONG).show()
            }
        } catch (e: JSONException) {
            Log.e("TAG", "error: ${e.message}")
            Toast.makeText(applicationContext, "Error parsing response: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientLoginContent(
    onLoginClick: (String, String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateAndLogin() {
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
        
        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 8) {
            passwordError = "Password must be at least 8 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (isValid) {
            onLoginClick(username, password)
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
                text = "Patient Portal",
                fontSize = 28.sp,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                text = "Sign in to track your health progress",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )
            
            Image(
                painter = painterResource(id = R.drawable.pic17),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    if (usernameError != null) usernameError = null
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                placeholder = { Text("Username") },
                singleLine = true,
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(
                            text = usernameError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (passwordError != null) passwordError = null
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.lock),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                placeholder = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = {
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Text(
                text = "Forgot Password?",
                color = Color(0xFFB80257),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, bottom = 24.dp)
                    .clickable { onForgotPasswordClick() }
            )
            
            Button(
                onClick = { validateAndLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2))
            ) {
                Text(
                    text = "Sign In", 
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Create Account",
                    color = Color(0xFFEB7DB2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onCreateAccountClick() }
                )
            }
        }
    }
}