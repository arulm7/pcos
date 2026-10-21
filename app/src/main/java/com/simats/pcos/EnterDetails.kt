package com.simats.pcos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class EnterDetails : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val usernameFromIntent = intent.getStringExtra("username") ?: ""

        setContent {
            val context = LocalContext.current
            EnterDetailsContent(
                initialName = usernameFromIntent,
                onSubmitClick = { details ->
                    if (validateFields(details)) {
                        sendDetailsToServer(details)
                    } else {
                        Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun validateFields(details: Map<String, String>): Boolean {
        return details.values.all { it.isNotBlank() }
    }

    private fun sendDetailsToServer(details: Map<String, String>) {
        val url = IpV4Connection.getUrl("personaldetails.php")
        val queue = Volley.newRequestQueue(this)

        val postRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                Log.e("Response", response.toString())
                val patientName = details["name"] ?: ""
                SessionManager.saveSession(this@EnterDetails, SessionManager.ROLE_PATIENT, patientName)
                val prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("details_completed_$patientName", true).apply()
                Toast.makeText(applicationContext, "Data submitted successfully!", Toast.LENGTH_LONG).show()
                val intent = Intent(this@EnterDetails, Welcome::class.java)
                intent.putExtra("username", details["name"])
                startActivity(intent)
                finish()
            },
            { error ->
                Toast.makeText(applicationContext, "Failed to submit data: $error", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return details
            }
        }
        queue.add(postRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterDetailsContent(
    initialName: String,
    onSubmitClick: (Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var age by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    
    val obstetricOptions = listOf("No Child", "Single Child", "Two Child", "More than 2 Child", "Unmarried")
    var expandedObstetric by remember { mutableStateOf(false) }
    var selectedObstetric by remember { mutableStateOf(obstetricOptions[0]) }

    val diseaseOptions = listOf("No Disease", "Hyperthyroidism", "Asthma", "Hypertension", "Diabetes", "Epilepsy")
    var expandedDisease by remember { mutableStateOf(false) }
    var selectedDisease by remember { mutableStateOf(diseaseOptions[0]) }
    
    var hipStr by remember { mutableStateOf("") }
    var waistStr by remember { mutableStateOf("") }

    // Calculated fields
    val bmi = remember(heightStr, weightStr) {
        val heightCm = heightStr.toDoubleOrNull() ?: 0.0
        val weightKg = weightStr.toDoubleOrNull() ?: 0.0
        if (heightCm > 0 && weightKg > 0) {
            val heightM = heightCm / 100
            String.format("%.2f", weightKg / (heightM * heightM))
        } else ""
    }

    val hipWaistRatio = remember(hipStr, waistStr) {
        val hip = hipStr.toDoubleOrNull() ?: 0.0
        val waist = waistStr.toDoubleOrNull() ?: 0.0
        if (hip > 0 && waist > 0) {
            String.format("%.2f", waist / hip)
        } else ""
    }

    val centralObesity = remember(hipWaistRatio) {
        val ratio = hipWaistRatio.toDoubleOrNull() ?: 0.0
        if (ratio > 0.85) "Yes" else if (ratio > 0.0) "No" else ""
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Enter The Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false // Match original behavior
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = mobileNo,
                onValueChange = { if (it.length <= 10) mobileNo = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight (Kgs)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            OutlinedTextField(
                value = bmi,
                onValueChange = {},
                label = { Text("BMI") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color(0xFFEB7DB2), // pink
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))
            
            ExposedDropdownMenuBox(
                expanded = expandedDisease,
                onExpandedChange = { expandedDisease = !expandedDisease }
            ) {
                OutlinedTextField(
                    value = selectedDisease,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Any Other Disease") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDisease) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedDisease,
                    onDismissRequest = { expandedDisease = false }
                ) {
                    diseaseOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedDisease = selectionOption
                                expandedDisease = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = expandedObstetric,
                onExpandedChange = { expandedObstetric = !expandedObstetric }
            ) {
                OutlinedTextField(
                    value = selectedObstetric,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Obstetric Score") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedObstetric) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedObstetric,
                    onDismissRequest = { expandedObstetric = false }
                ) {
                    obstetricOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedObstetric = selectionOption
                                expandedObstetric = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hipStr,
                    onValueChange = { hipStr = it },
                    label = { Text("Hip (Cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = waistStr,
                    onValueChange = { waistStr = it },
                    label = { Text("Waist (Cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            OutlinedTextField(
                value = hipWaistRatio,
                onValueChange = {},
                label = { Text("Hip Waist Ratio") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            OutlinedTextField(
                value = centralObesity,
                onValueChange = {},
                label = { Text("Central Obesity") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    val details = mapOf(
                        "name" to name,
                        "age" to age,
                        "Mobile_No" to mobileNo,
                        "height" to heightStr,
                        "weight" to weightStr,
                        "bmi" to bmi,
                        "otherdisease" to selectedDisease,
                        "obstetricscore" to selectedObstetric,
                        "hip" to hipStr,
                        "waist" to waistStr,
                        "hipwaist" to hipWaistRatio,
                        "centralobesity" to centralObesity
                    )
                    onSubmitClick(details)
                },
                modifier = Modifier.size(width = 150.dp, height = 55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)) // @color/pink
            ) {
                Text(text = "Submit", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}