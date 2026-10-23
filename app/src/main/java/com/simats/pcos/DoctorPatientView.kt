package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

class DoctorPatientView : ComponentActivity() {

    private var patientName by mutableStateOf("")
    private var patientProfileImage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        patientName = intent.getStringExtra("patient_name") ?: ""
        if (patientName.isNotEmpty()) {
            fetchPatientProfile(patientName)
        }

        setContent {
            var showDeleteDialog by remember { mutableStateOf(false) }

            DoctorPatientViewScreen(
                patientName = patientName,
                patientProfileImage = patientProfileImage,
                onBackClick = { finish() },
                onNavigate = { destination ->
                    val intent = Intent(this, destination)
                    intent.putExtra("patient_name", patientName)
                    intent.putExtra("username", patientName)
                    startActivity(intent)
                },
                onDeleteClick = { showDeleteDialog = true }
            )

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            text = "Delete Patient",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to remove $patientName from your patient roster? This action cannot be undone.",
                            color = Color(0xFF64748B)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                executeDeleteRequest(patientName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }

    private fun executeDeleteRequest(name: String) {
        val url = IpV4Connection.getUrl("delete.php?username=") + name
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val clean = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(clean)
                    if (jsonResponse.optString("status") == "success" || jsonResponse.optBoolean("success", false)) {
                        Toast.makeText(this@DoctorPatientView, "Patient deleted successfully.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DoctorHomepage::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@DoctorPatientView, "Error: ${jsonResponse.optString("message")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this@DoctorPatientView, "Error parsing server response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this@DoctorPatientView, "Failed to communicate with server: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(stringRequest)
    }

    private fun fetchPatientProfile(name: String) {
        val url = IpV4Connection.getUrl("profile.php?name=") + name
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val clean = IpV4Connection.cleanJsonResponse(response)
                    val json = JSONObject(clean)
                    if (json.optBoolean("success", false) || json.has("patient_details")) {
                        val details = json.getJSONObject("patient_details")
                        val img = details.optString("profile_image", "").takeIf { it.isNotBlank() }
                        patientProfileImage = ImageHelper.normalizeImageUrl(img)
                    }
                } catch (_: Exception) {}
            },
            { /* silent */ }
        )
        Volley.newRequestQueue(this).add(request)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientViewScreen(
    patientName: String,
    patientProfileImage: String? = null,
    onBackClick: () -> Unit,
    onNavigate: (Class<*>) -> Unit,
    onDeleteClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patient Overview",
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
                actions = {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Patient",
                            tint = Color(0xFFEF4444)
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
        ) {
            // Hero Patient Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
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
                    ProfileAvatar(
                        imageUrl = patientProfileImage,
                        name = patientName,
                        size = 60.dp,
                        borderWidth = 2.dp,
                        borderColor = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = patientName.ifEmpty { "Patient" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Active Patient",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Clinical Actions",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Grid of Actions
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    DoctorActionCard(
                        title = "Today's Progress",
                        subtitle = "Steps, calories, sleep",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = Color(0xFF10B981),
                        onClick = { onNavigate(TodayProgressDoctor::class.java) }
                    )
                }
                item {
                    DoctorActionCard(
                        title = "Medical Reports",
                        subtitle = "Clinical test files",
                        icon = Icons.Default.Description,
                        accentColor = Color(0xFF0284C7),
                        onClick = { onNavigate(MedicalReports::class.java) }
                    )
                }
                item {
                    DoctorActionCard(
                        title = "Patient Profile",
                        subtitle = "BMI, health details",
                        icon = Icons.Default.Badge,
                        accentColor = Color(0xFF8B5CF6),
                        onClick = { onNavigate(PatientProfile::class.java) }
                    )
                }
                item {
                    DoctorActionCard(
                        title = "Track Symptoms",
                        subtitle = "Symptom timeline",
                        icon = Icons.Default.Insights,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { onNavigate(TrackView::class.java) }
                    )
                }
                item {
                    DoctorActionCard(
                        title = "Menstrual Cycle",
                        subtitle = "Period logs & calendar",
                        icon = Icons.Default.CalendarMonth,
                        accentColor = Color(0xFFEC4899),
                        onClick = { onNavigate(PatientMenstrualCalenderActivity::class.java) }
                    )
                }
                item {
                    DoctorActionCard(
                        title = "Q&A Assessment",
                        subtitle = "Self assessment data",
                        icon = Icons.Default.Quiz,
                        accentColor = Color(0xFF6366F1),
                        onClick = { onNavigate(PatientAnswersActivity::class.java) }
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clickable(onClick = onClick),
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
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
