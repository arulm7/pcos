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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class PatientProfile : ComponentActivity() {

    private var profileData by mutableStateOf<ProfileData?>(null)
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawName = intent.getStringExtra("username")
            ?: intent.getStringExtra("patient_name")
            ?: SessionManager.getUsername(this)

        currentUsername = rawName.trim()

        if (currentUsername.isNotEmpty()) {
            fetchPatientDetails(currentUsername)
        } else {
            errorMessage = "No patient identifier found"
        }

        setContent {
            PatientProfileScreen(
                profileData = profileData,
                username = currentUsername,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBackClick = { finish() },
                onRefresh = {
                    if (currentUsername.isNotEmpty()) fetchPatientDetails(currentUsername)
                },
                onEditClick = {
                    val intent = Intent(this@PatientProfile, PatientEditProfileActivity::class.java).apply {
                        putExtra("username", currentUsername)
                        putExtra("patient_name", currentUsername)
                    }
                    startActivity(intent)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUsername.isNotEmpty()) {
            fetchPatientDetails(currentUsername)
        }
    }

    private fun fetchPatientDetails(name: String) {
        isLoading = true
        errorMessage = null
        val url = IpV4Connection.getUrl("profile.php?name=") + name
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                isLoading = false
                try {
                    val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(cleanResponse)
                    if (jsonResponse.optBoolean("success", false) || jsonResponse.has("patient_details")) {
                        val details = jsonResponse.getJSONObject("patient_details")

                        profileData = ProfileData(
                            name = details.optString("name", name),
                            age = details.optInt("age", 0).let { if (it > 0) it.toString() else details.optString("age", "") },
                            mobileNo = details.optString("Mobile_No", ""),
                            height = details.optString("height", ""),
                            weight = details.optString("weight", ""),
                            bmi = details.optString("bmi", ""),
                            otherDisease = details.optString("otherdisease", ""),
                            obstetricScore = details.optString("obstetricscore", ""),
                            hip = details.optString("hip", ""),
                            waist = details.optString("waist", ""),
                            hipWaist = details.optString("hipwaist", ""),
                            centralObesity = details.optString("centralobesity", ""),
                            profileImage = details.optString("profile_image", "")
                        )
                    } else {
                        val msg = jsonResponse.optString("message", "Could not load patient details")
                        errorMessage = msg
                    }
                } catch (e: Exception) {
                    errorMessage = "Error parsing profile data: ${e.message}"
                }
            },
            { error ->
                isLoading = false
                errorMessage = "Failed to load profile: ${error.message ?: "Network error"}"
            }
        )
        queue.add(stringRequest)
    }
}

data class ProfileData(
    val name: String = "",
    val age: String = "",
    val mobileNo: String = "",
    val height: String = "",
    val weight: String = "",
    val bmi: String = "",
    val otherDisease: String = "",
    val obstetricScore: String = "",
    val hip: String = "",
    val waist: String = "",
    val hipWaist: String = "",
    val centralObesity: String = "",
    val profileImage: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    profileData: ProfileData?,
    username: String,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patient Profile",
                        fontWeight = FontWeight.Bold,
                        color = PcosTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PcosTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PcosTextSecondary
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = PcosPrimaryPink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = PcosBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && profileData == null -> {
                    LoadingContent(
                        message = "Loading profile...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null && profileData == null -> {
                    ErrorStateContent(
                        errorMessage = errorMessage,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Profile Avatar using shared modern component
                        ProfileAvatar(
                            imageUrl = profileData?.profileImage,
                            name = profileData?.name?.ifEmpty { username } ?: username,
                            size = 110.dp,
                            borderWidth = 3.dp,
                            borderColor = PcosPrimaryPink
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = profileData?.name?.ifEmpty { username } ?: username,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PcosTextPrimary
                        )

                        Text(
                            text = "Patient Roster",
                            fontSize = 13.sp,
                            color = PcosTextSecondary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Profile Details Section Card
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Personal Information",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PcosTextPrimary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                ProfileInfoRow("Full Name", profileData?.name?.ifEmpty { "-" } ?: "-")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Age", profileData?.age?.ifEmpty { "-" } ?: "-")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Mobile Number", profileData?.mobileNo?.ifEmpty { "-" } ?: "-")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Any Other Disease", profileData?.otherDisease?.ifEmpty { "None" } ?: "None")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Obstetric Score", profileData?.obstetricScore?.ifEmpty { "-" } ?: "-")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Body & Health Metrics Card
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Physical & Biometric Metrics",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PcosTextPrimary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                ProfileInfoRow("Height", "${profileData?.height?.ifEmpty { "-" }} cm")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Weight", "${profileData?.weight?.ifEmpty { "-" }} kg")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("BMI", profileData?.bmi?.ifEmpty { "-" } ?: "-")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Hip", "${profileData?.hip?.ifEmpty { "-" }} cm")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Waist", "${profileData?.waist?.ifEmpty { "-" }} cm")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Hip-Waist Ratio", profileData?.hipWaist?.ifEmpty { "-" } ?: "-")
                                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("Central Obesity", profileData?.centralObesity?.ifEmpty { "-" } ?: "-")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = PcosTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = PcosTextPrimary
        )
    }
}
