package com.simats.pcos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

data class TodayProgressData(
    val date: String = "",
    val calories: Int = 0,
    val steps: Int = 0,
    val exerciseMinutes: Int = 0,
    val sleepHours: String = ""
)

class TodayProgressDoctor : ComponentActivity() {

    private var patientName by mutableStateOf("")
    private var progressData by mutableStateOf<TodayProgressData?>(null)
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        patientName = intent.getStringExtra("patient_name") ?: ""

        if (patientName.isNotEmpty()) {
            fetchTodayProgress(patientName)
        } else {
            Toast.makeText(this, "No patient name provided!", Toast.LENGTH_SHORT).show()
        }

        setContent {
            TodayProgressDoctorScreen(
                patientName = patientName,
                data = progressData,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBackClick = { finish() },
                onRefresh = {
                    if (patientName.isNotEmpty()) fetchTodayProgress(patientName)
                }
            )
        }
    }

    private fun fetchTodayProgress(name: String) {
        isLoading = true
        errorMessage = null
        val url = IpV4Connection.getUrl("todayprogress-D.php?name=") + name
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                isLoading = false
                try {
                    val clean = IpV4Connection.cleanJsonResponse(response)
                    val jsonObject = JSONObject(clean)
                    if (jsonObject.getBoolean("success")) {
                        val dataObj = jsonObject.getJSONObject("data")
                        progressData = TodayProgressData(
                            date = dataObj.optString("Date", "Today"),
                            calories = dataObj.optInt("calories_taken", 0),
                            steps = dataObj.optInt("no_of_steps", 0),
                            exerciseMinutes = dataObj.optInt("exercise_duration", 0),
                            sleepHours = dataObj.optString("todays_feedback", "0")
                        )
                    } else {
                        val msg = jsonObject.optString("message", "No progress records found for today")
                        errorMessage = msg
                        Toast.makeText(this@TodayProgressDoctor, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    errorMessage = "Error parsing server data"
                    Toast.makeText(this@TodayProgressDoctor, "Error parsing server data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                isLoading = false
                errorMessage = "Failed to connect to server"
                Toast.makeText(this@TodayProgressDoctor, "Failed to connect to server: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(stringRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayProgressDoctorScreen(
    patientName: String,
    data: TodayProgressData?,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Today's Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Patient: $patientName",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
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
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFEB7DB2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFEB7DB2)
                )
            } else if (errorMessage != null && data == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 16.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                val currentData = data ?: TodayProgressData()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = patientName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Date: ${if (currentData.date.isNotEmpty()) currentData.date else "Today"}",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Activity & Health Metrics",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid of 4 polished metric cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProgressMetricCard(
                            title = "Calories Taken",
                            value = "${currentData.calories}",
                            unit = "kcal",
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = Color(0xFFF97316),
                            modifier = Modifier.weight(1f)
                        )
                        ProgressMetricCard(
                            title = "Steps Walked",
                            value = "${currentData.steps}",
                            unit = "steps",
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            accentColor = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProgressMetricCard(
                            title = "Exercise",
                            value = "${currentData.exerciseMinutes}",
                            unit = "mins",
                            icon = Icons.Default.FitnessCenter,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        ProgressMetricCard(
                            title = "Sleeping Hours",
                            value = currentData.sleepHours,
                            unit = "hours",
                            icon = Icons.Default.Bedtime,
                            accentColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Status Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Compliance Status",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (currentData.steps > 0 || currentData.calories > 0)
                                    "Patient has logged their routine for ${currentData.date}. Exercise and sleep parameters are recorded."
                                else
                                    "Awaiting daily log submission from the patient.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ProgressMetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = value.ifEmpty { "0" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
