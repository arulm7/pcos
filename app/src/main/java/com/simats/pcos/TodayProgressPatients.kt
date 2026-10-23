package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayProgressPatients : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""
        val initialCalories = intent.getIntExtra("total_calories", 0)

        setContent {
            TodayProgressPatientsScreen(
                username = username,
                initialCalories = if (initialCalories > 0) initialCalories.toString() else "",
                onBackClick = { finish() },
                onSubmit = { details ->
                    submitData(details)
                }
            )
        }
    }

    private fun submitData(paramsMap: Map<String, String>) {
        val url = IpV4Connection.getUrl("todayprogress.php")
        val queue = Volley.newRequestQueue(this)

        val postRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val clean = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(clean)
                    val success = jsonResponse.optBoolean("success", false)
                    val message = jsonResponse.optString("message", "Submitted successfully")
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

                    if (success) {
                        val intent = Intent(this@TodayProgressPatients, PatientHome::class.java)
                        intent.putExtra("username", paramsMap["name"])
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Error parsing server response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(applicationContext, "Failed to submit: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> = paramsMap
        }
        queue.add(postRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayProgressPatientsScreen(
    username: String,
    initialCalories: String,
    onBackClick: () -> Unit,
    onSubmit: (Map<String, String>) -> Unit
) {
    val defaultDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var date by remember { mutableStateOf(defaultDate) }
    var calories by remember { mutableStateOf(initialCalories) }
    var steps by remember { mutableStateOf("") }
    var exerciseDuration by remember { mutableStateOf("") }
    var sleepingHours by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Log Today's Routine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1E293B)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFEB7DB2), Color(0xFFFFA2CF))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Hello, ${username.ifEmpty { "Patient" }}!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record your health vitals and daily habits to track your PCOS progress.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form inputs
            ProgressInputCard(
                label = "Date (YYYY-MM-DD)",
                value = date,
                onValueChange = { date = it },
                icon = Icons.Default.CalendarToday,
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(14.dp))

            ProgressInputCard(
                label = "Calories Consumed (Kcal)",
                value = calories,
                onValueChange = { calories = it },
                icon = Icons.Default.LocalFireDepartment,
                keyboardType = KeyboardType.Number,
                placeholder = "e.g. 1800"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ProgressInputCard(
                label = "Number of Steps",
                value = steps,
                onValueChange = { steps = it },
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                keyboardType = KeyboardType.Number,
                placeholder = "e.g. 6000"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ProgressInputCard(
                label = "Exercise Duration (Minutes)",
                value = exerciseDuration,
                onValueChange = { exerciseDuration = it },
                icon = Icons.Default.FitnessCenter,
                keyboardType = KeyboardType.Number,
                placeholder = "e.g. 30"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ProgressInputCard(
                label = "Sleeping Hours",
                value = sleepingHours,
                onValueChange = { sleepingHours = it },
                icon = Icons.Default.Bedtime,
                keyboardType = KeyboardType.Number,
                placeholder = "e.g. 8"
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    val params = mapOf(
                        "name" to username,
                        "calories_taken" to calories,
                        "no_of_steps" to steps,
                        "exercise_duration" to exerciseDuration,
                        "todays_feedback" to sleepingHours,
                        "date" to date
                    )
                    onSubmit(params)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2))
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Today's Record",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProgressInputCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFEB7DB2),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, fontSize = 13.sp) },
                placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFEB7DB2),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
