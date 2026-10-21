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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit

class PatientProfile : ComponentActivity() {

    private var profileData by mutableStateOf<ProfileData?>(null)
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        currentUsername = intent.getStringExtra("username") ?: intent.getStringExtra("patient_name") ?: ""
        if (currentUsername.isNotEmpty()) {
            fetchPatientDetails(currentUsername)
        }

        setContent {
            PatientProfileContent(
                profileData = profileData,
                onBackClick = { finish() },
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
        val url = IpV4Connection.getUrl("profile.php?name=") + name
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(cleanResponse)
                    if (jsonResponse.getBoolean("success")) {
                        val details = jsonResponse.getJSONObject("patient_details")
                        
                        profileData = ProfileData(
                            name = details.optString("name", ""),
                            age = details.optInt("age", 0).toString(),
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
                        Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to fetch data: ${error.message}", Toast.LENGTH_LONG).show()
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
fun PatientProfileContent(
    profileData: ProfileData?,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.Bold,
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
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color(0xFFEB7DB2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val imageUrl = profileData?.profileImage?.takeIf { it.isNotEmpty() }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                300, 300 // Approximately 100dp depending on density, but better to set exact dp in Compose
                            )
                        }
                    },
                    update = { imageView ->
                        if (imageUrl != null) {
                            com.bumptech.glide.Glide.with(imageView.context)
                                .load(imageUrl)
                                .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.icon_profile)
                                .error(R.drawable.icon_profile)
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.group35)
                        }
                    },
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                ProfileField("Name", profileData?.name ?: "")
                ProfileField("Age", profileData?.age ?: "")
                ProfileField("Mobile Number", profileData?.mobileNo ?: "")
                ProfileField("Height", profileData?.height ?: "")
                ProfileField("Weight", profileData?.weight ?: "")
                ProfileField("BMI", profileData?.bmi ?: "")
                ProfileField("Any other disease", profileData?.otherDisease ?: "")
                ProfileField("Obstetric Score", profileData?.obstetricScore ?: "")
                ProfileField("Hip (cm)", profileData?.hip ?: "")
                ProfileField("Waist (cm)", profileData?.waist ?: "")
                ProfileField("Hip Waist Ratio", profileData?.hipWaist ?: "")
                ProfileField("Central Obesity", profileData?.centralObesity ?: "")
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .width(180.dp)
                .height(50.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.Black),
            singleLine = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PatientProfileContentPreview() {
    PatientProfileContent(
        profileData = ProfileData(
            name = "John Doe",
            age = "25",
            mobileNo = "1234567890",
            height = "175",
            weight = "70",
            bmi = "22.8",
            otherDisease = "None",
            obstetricScore = "0",
            hip = "90",
            waist = "80",
            hipWaist = "0.88",
            centralObesity = "No"
        )
    )
}
