package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

class AdminPageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdminPageContent(
                onLoginClick = { username, password ->
                    loginUser(username, password)
                }
            )
        }
    }

    private fun loginUser(username: String, password: String) {
        val queue = Volley.newRequestQueue(this)
        val url = IpV4Connection.getUrl("admin_login.php")

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(cleanResponse)
                    val success = jsonResponse.getBoolean("success")
                    val message = jsonResponse.getString("message")
                    if (success) {
                        SessionManager.saveSession(this@AdminPageActivity, SessionManager.ROLE_ADMIN, username)
                        Toast.makeText(this@AdminPageActivity, message, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@AdminPageActivity, AddDoctorsActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@AdminPageActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this@AdminPageActivity, "JSON error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this@AdminPageActivity, "Volley error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["username"] = username
                params["password"] = password
                return params
            }
        }
        queue.add(stringRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPageContent(
    onLoginClick: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Admin Login",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(50.dp))
            
            Image(
                painter = painterResource(id = R.drawable.pic17),
                contentDescription = null
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .width(250.dp)
                    .height(60.dp),
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp)
                    )
                },
                placeholder = { Text("USERNAME") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(15.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .width(250.dp)
                    .height(60.dp),
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.lock),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp)
                    )
                },
                placeholder = { Text("PASSWORD") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Button(
                onClick = { onLoginClick(username, password) },
                modifier = Modifier.size(width = 150.dp, height = 55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)) // @color/pink
            ) {
                Text(text = "Login", color = Color.White)
            }
        }
    }
}