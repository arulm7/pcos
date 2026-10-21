package com.simats.pcos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLEncoder

class PatientEditProfileActivity : ComponentActivity() {

    private var username: String = ""
    private var profileImageUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        username = intent.getStringExtra("username") ?: intent.getStringExtra("patient_name") ?: ""

        if (username.isEmpty()) {
            Toast.makeText(this, "Patient name is missing!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    uploadImage(it, username)
                }
            }

            PatientEditProfileScreen(
                username = username,
                profileImageUrl = profileImageUrl,
                onBackClick = { finish() },
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onSave = { params ->
                    submitDetails(params)
                },
                onFetchData = { callback ->
                    fetchPatientDetails(username, callback)
                }
            )
        }
    }

    private fun fetchPatientDetails(name: String, onLoaded: (Map<String, String>, String?) -> Unit) {
        try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val url = IpV4Connection.getUrl("profile.php?name=") + encodedName

            val stringRequest = StringRequest(
                Request.Method.GET, url,
                { response ->
                    try {
                        val clean = IpV4Connection.cleanJsonResponse(response)
                        val jsonResponse = JSONObject(clean)
                        if (jsonResponse.getBoolean("success")) {
                            val details = jsonResponse.getJSONObject("patient_details")
                            val map = mutableMapOf<String, String>()
                            map["name"] = details.optString("name", name)
                            map["age"] = details.optInt("age", 0).let { if (it > 0) it.toString() else "" }
                            map["Mobile_No"] = details.optString("Mobile_No", "")
                            map["height"] = details.optString("height", "")
                            map["weight"] = details.optString("weight", "")
                            map["bmi"] = details.optString("bmi", "")
                            map["otherdisease"] = details.optString("otherdisease", "")
                            map["obstetricscore"] = details.optString("obstetricscore", "")
                            map["hip"] = details.optString("hip", "")
                            map["waist"] = details.optString("waist", "")
                            map["hipwaist"] = details.optString("hipwaist", "")
                            map["centralobesity"] = details.optString("centralobesity", "")

                            val img = details.optString("profile_image", "").takeIf { it.isNotBlank() }
                            profileImageUrl = img
                            onLoaded(map, img)
                        } else {
                            Toast.makeText(applicationContext, jsonResponse.optString("message", "Could not fetch details"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(applicationContext, "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
            Volley.newRequestQueue(this).add(stringRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadImage(imageUri: Uri, username: String) {
        val url = IpV4Connection.getUrl("profileimageA.php")
        val base64 = imageToBase64(imageUri) ?: return

        val imageUploadRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val clean = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(clean)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(applicationContext, jsonResponse.optString("message", "Image uploaded successfully!"), Toast.LENGTH_LONG).show()
                        profileImageUrl = if (jsonResponse.has("image_url")) jsonResponse.getString("image_url") else null
                    } else {
                        Toast.makeText(applicationContext, jsonResponse.optString("error", "Upload failed"), Toast.LENGTH_LONG).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(applicationContext, "Error parsing JSON: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(applicationContext, "Error during image upload: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "name" to username,
                    "image" to base64
                )
            }
        }

        Volley.newRequestQueue(this).add(imageUploadRequest)
    }

    private fun imageToBase64(imageUri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            val imageBytes = outputStream.toByteArray()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun submitDetails(params: Map<String, String>) {
        val url = IpV4Connection.getUrl("edit.php")

        val postRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val clean = IpV4Connection.cleanJsonResponse(response)
                    val jsonResponse = JSONObject(clean)
                    val msg = jsonResponse.optString("message", "Profile updated successfully!")
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    if (jsonResponse.optBoolean("success", false) || jsonResponse.optString("status") == "success") {
                        finish()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(applicationContext, "JSON parsing error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(applicationContext, "Error saving profile: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> = params
        }

        Volley.newRequestQueue(this).add(postRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditProfileScreen(
    username: String,
    profileImageUrl: String?,
    onBackClick: () -> Unit,
    onPickImage: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    onFetchData: ((Map<String, String>, String?) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(username) }
    var age by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var otherDisease by remember { mutableStateOf("") }
    var obstetricScore by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }

    // Calculated values
    val bmi = remember(height, weight) {
        val h = height.toDoubleOrNull() ?: 0.0
        val w = weight.toDoubleOrNull() ?: 0.0
        if (h > 0 && w > 0) {
            val hm = h / 100.0
            String.format(java.util.Locale.US, "%.2f", w / (hm * hm))
        } else ""
    }

    val hipWaistRatio = remember(hip, waist) {
        val h = hip.toDoubleOrNull() ?: 0.0
        val w = waist.toDoubleOrNull() ?: 0.0
        if (h > 0 && w > 0) {
            String.format(java.util.Locale.US, "%.2f", w / h)
        } else ""
    }

    val centralObesity = remember(hipWaistRatio) {
        val ratio = hipWaistRatio.toDoubleOrNull() ?: 0.0
        if (ratio > 0.85) "Yes" else if (ratio > 0.0) "No" else ""
    }

    LaunchedEffect(Unit) {
        onFetchData { map, _ ->
            name = map["name"] ?: username
            age = map["age"] ?: ""
            mobileNo = map["Mobile_No"] ?: ""
            height = map["height"] ?: ""
            weight = map["weight"] ?: ""
            otherDisease = map["otherdisease"] ?: ""
            obstetricScore = map["obstetricscore"] ?: ""
            hip = map["hip"] ?: ""
            waist = map["waist"] ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image Picker
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .clickable { onPickImage() }
            ) {
                if (!profileImageUrl.isNullOrEmpty()) {
                    AndroidView(
                        factory = { context ->
                            android.widget.ImageView(context).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            Glide.with(imageView.context)
                                .load(profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.icon_profile)
                                .error(R.drawable.icon_profile)
                                .into(imageView)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEB7DB2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap to change photo",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Editable Input Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = mobileNo,
                onValueChange = { mobileNo = it },
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bmi,
                onValueChange = {},
                readOnly = true,
                label = { Text("BMI (Calculated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hip,
                    onValueChange = { hip = it },
                    label = { Text("Hip (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = waist,
                    onValueChange = { waist = it },
                    label = { Text("Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hipWaistRatio,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hip-Waist Ratio") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = centralObesity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Central Obesity") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = otherDisease,
                onValueChange = { otherDisease = it },
                label = { Text("Other Disease") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = obstetricScore,
                onValueChange = { obstetricScore = it },
                label = { Text("Obstetric Score") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val map = hashMapOf(
                        "name" to name,
                        "age" to age,
                        "Mobile_No" to mobileNo,
                        "height" to height,
                        "weight" to weight,
                        "bmi" to bmi,
                        "otherdisease" to otherDisease,
                        "obstetricscore" to obstetricScore,
                        "hip" to hip,
                        "waist" to waist,
                        "hipwaist" to hipWaistRatio,
                        "centralobesity" to centralObesity
                    )
                    onSave(map)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Save Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
