package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class DoctorHomepage : ComponentActivity() {
    private val patientList = mutableStateListOf<Patient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fetchPatients()
        
        setContent {
            DoctorHomepageContent(
                patientList = patientList,
                onAddPatientClick = {
                    startActivity(Intent(this, AddPatients::class.java))
                },
                onLogoutClick = {
                    confirmLogout()
                },
                onPatientClick = { patient ->
                    val intent = Intent(this, DoctorPatientView::class.java)
                    intent.putExtra("patient_name", patient.username)
                    startActivity(intent)
                }
            )
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("OK") { _, _ -> logout() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun logout() {
        SessionManager.clearSession(this)
        val intent = Intent(this, RollSelection::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun fetchPatients() {
        val url = IpV4Connection.getUrl("patientlist.php")
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                    val jsonObject = JSONObject(cleanResponse)
                    if (jsonObject.getBoolean("success")) {
                        val usernames = jsonObject.getJSONArray("usernames")
                        patientList.clear()
                        for (i in 0 until usernames.length()) {
                            patientList.add(Patient(usernames.getString(i)))
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }
        )
        Volley.newRequestQueue(this).add(stringRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorHomepageContent(
    patientList: List<Patient>,
    onAddPatientClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPatientClick: (Patient) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredPatients = remember(searchQuery, patientList) {
        if (searchQuery.isBlank()) {
            patientList
        } else {
            patientList.filter { 
                it.username.lowercase(Locale.getDefault()).contains(searchQuery.lowercase(Locale.getDefault()))
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            // Top Bar section with search and logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(55.dp),
                    placeholder = { Text("Search patients") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFFFA2CF) // @color/Another
                    )
                }
            }
            
            // List area
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .alpha(0.3f),
                    contentScale = ContentScale.Fit
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredPatients) { patient ->
                        PatientCell(
                            patient = patient,
                            onClick = { onPatientClick(patient) }
                        )
                        HorizontalDivider(color = Color.LightGray)
                    }
                }
            }
            
            // Bottom area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFC0DE)) // @color/ViewBack
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Patient Here",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onAddPatientClick,
                    modifier = Modifier
                        .size(width = 280.dp, height = 60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)) // @color/pink
                ) {
                    Text(text = "Add Patient", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(15.dp))
            }
        }
    }
}

@Composable
fun PatientCell(
    patient: Patient,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = patient.username,
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}