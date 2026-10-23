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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class New3Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""

        setContent {
            WellnessFeedbackScreen(
                username = username,
                onBack = { finish() },
                onSuccess = {
                    val homeIntent = Intent(this, PatientHome::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessFeedbackScreen(
    username: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mealPlanFollowed by remember { mutableStateOf("Yes") }
    var mealPercentage by remember { mutableFloatStateOf(80f) }

    var exercisePlanFollowed by remember { mutableStateOf("Yes") }
    var exercisePercentage by remember { mutableFloatStateOf(70f) }

    var meditationRoutineFollowed by remember { mutableStateOf("Yes") }
    var meditationPercentage by remember { mutableFloatStateOf(60f) }

    var isSubmitting by remember { mutableStateOf(false) }

    val primaryPink = Color(0xFFEB7DB2)
    val darkPink = Color(0xFFB80257)

    fun submitFeedback() {
        isSubmitting = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val params = JSONObject().apply {
                    put("username", username)
                    put("meal_plan", mealPlanFollowed)
                    put("meal_percentage", mealPercentage.toInt().toString())
                    put("exercise_plan", exercisePlanFollowed)
                    put("exercise_percentage", exercisePercentage.toInt().toString())
                    put("meditation_plan", meditationRoutineFollowed)
                    put("meditation_percentage", meditationPercentage.toInt().toString())
                }

                val url = URL(IpV4Connection.getUrl("health_follow.php"))
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                connection.outputStream.use { os: OutputStream ->
                    os.write(params.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = connection.responseCode
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(context, "Failed to submit data ($responseCode)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Daily Routine Feedback",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkPink)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFBF8F9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card with patient name
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hello, $username",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = darkPink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record your adherence to today's personalized PCOS lifestyle plans.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Section 1: Meal Plan
            AdherenceSectionCard(
                title = "Meal Plan Adherence",
                icon = Icons.Default.Restaurant,
                iconColor = Color(0xFFFF9800),
                isFollowed = mealPlanFollowed,
                onFollowedChanged = { mealPlanFollowed = it },
                percentage = mealPercentage,
                onPercentageChanged = { mealPercentage = it }
            )

            // Section 2: Exercise Plan
            AdherenceSectionCard(
                title = "Exercise Routine Adherence",
                icon = Icons.Default.FitnessCenter,
                iconColor = Color(0xFF4CAF50),
                isFollowed = exercisePlanFollowed,
                onFollowedChanged = { exercisePlanFollowed = it },
                percentage = exercisePercentage,
                onPercentageChanged = { exercisePercentage = it }
            )

            // Section 3: Meditation
            AdherenceSectionCard(
                title = "Meditation & Mindfulness",
                icon = Icons.Default.SelfImprovement,
                iconColor = Color(0xFF9C27B0),
                isFollowed = meditationRoutineFollowed,
                onFollowedChanged = { meditationRoutineFollowed = it },
                percentage = meditationPercentage,
                onPercentageChanged = { meditationPercentage = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = { submitFeedback() },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryPink)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Routine Feedback",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdherenceSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isFollowed: String,
    onFollowedChanged: (String) -> Unit,
    percentage: Float,
    onPercentageChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF222222)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Followed Yes/No Segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Did you follow this routine?",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isFollowed == "Yes",
                        onClick = { onFollowedChanged("Yes") },
                        label = { Text("Yes", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = isFollowed == "No",
                        onClick = { onFollowedChanged("No") },
                        label = { Text("No", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Percentage Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Completion Level", fontSize = 13.sp, color = Color.Gray)
                Text(
                    text = "${percentage.toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            Slider(
                value = percentage,
                onValueChange = onPercentageChanged,
                valueRange = 0f..100f,
                steps = 19, // 5% increments
                colors = SliderDefaults.colors(
                    thumbColor = iconColor,
                    activeTrackColor = iconColor,
                    inactiveTrackColor = iconColor.copy(alpha = 0.2f)
                )
            )
        }
    }
}
