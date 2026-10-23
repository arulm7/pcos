package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var doctorName by mutableStateOf("Doctor")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val passedName = intent.getStringExtra("doctor_id")
            ?: intent.getStringExtra("username")
            ?: SessionManager.getUsername(this)

        if (!passedName.isNullOrBlank()) {
            doctorName = passedName
        }

        fetchPatients()

        setContent {
            DoctorDashboardScreen(
                doctorName = doctorName,
                patientList = patientList,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onRefresh = { fetchPatients() },
                onAddPatientClick = {
                    startActivity(Intent(this, AddPatients::class.java))
                },
                onLogoutClick = {
                    confirmLogout()
                },
                onPatientClick = { patient ->
                    val intent = Intent(this, DoctorPatientView::class.java).apply {
                        putExtra("patient_name", patient.username)
                        putExtra("username", patient.username)
                    }
                    startActivity(intent)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh patient roster when returning (e.g., after adding a new patient)
        if (patientList.isEmpty() && !isLoading) {
            fetchPatients()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from Doctor Portal?")
            .setPositiveButton("Logout") { _, _ -> logout() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun logout() {
        SessionManager.clearSession(this)
        val intent = Intent(this, RollSelection::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun fetchPatients() {
        isLoading = true
        errorMessage = null
        val url = IpV4Connection.getUrl("patientlist.php")

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                isLoading = false
                try {
                    val cleanResponse = IpV4Connection.cleanJsonResponse(response)
                    val jsonObject = JSONObject(cleanResponse)
                    if (jsonObject.optBoolean("success", false) || jsonObject.has("usernames")) {
                        val usernames = jsonObject.getJSONArray("usernames")
                        val newList = mutableListOf<Patient>()
                        for (i in 0 until usernames.length()) {
                            val name = usernames.getString(i)
                            if (name.isNotBlank()) {
                                newList.add(Patient(name))
                            }
                        }
                        patientList.clear()
                        patientList.addAll(newList)
                    } else {
                        errorMessage = jsonObject.optString("message", "Could not fetch patient list")
                    }
                } catch (e: JSONException) {
                    errorMessage = "Error parsing server response: ${e.message}"
                }
            },
            { error ->
                isLoading = false
                errorMessage = "Network error: ${error.message ?: "Could not connect to server"}"
            }
        )
        Volley.newRequestQueue(this).add(stringRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    doctorName: String,
    patientList: List<Patient>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onAddPatientClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPatientClick: (Patient) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredPatients = remember(searchQuery, patientList) {
        if (searchQuery.isBlank()) {
            patientList
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            patientList.filter {
                it.username.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            imageUrl = null,
                            name = doctorName,
                            size = 40.dp,
                            borderColor = PcosPrimaryPink
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dr. ${doctorName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = PcosTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "PCOS Clinical Specialist",
                                fontSize = 12.sp,
                                color = PcosTextSecondary
                            )
                        }
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
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPatientClick,
                containerColor = PcosPrimaryPink,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Patient", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = PcosBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Summary Cards Header
            item(key = "dashboard_summary") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Clinical Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PcosTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardSummaryCard(
                            title = "Total Patients",
                            value = patientList.size.toString(),
                            icon = Icons.Default.People,
                            accentColor = Color(0xFF0284C7),
                            subtitle = "Enrolled roster",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardSummaryCard(
                            title = "Active Today",
                            value = patientList.size.toString(),
                            icon = Icons.Default.CheckCircle,
                            accentColor = Color(0xFF10B981),
                            subtitle = "Monitoring active",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search patient by username...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PcosTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = PcosTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PcosPrimaryPink,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            // Roster Section Header
            item(key = "roster_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Patient Roster (${filteredPatients.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PcosTextPrimary
                    )

                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "Filtered from ${patientList.size}",
                            fontSize = 12.sp,
                            color = PcosTextSecondary
                        )
                    }
                }
            }

            // State Content: Loading, Error, Empty, or List
            when {
                isLoading && patientList.isEmpty() -> {
                    item(key = "state_loading") {
                        LoadingContent(
                            message = "Fetching patient roster...",
                            modifier = Modifier.fillParentMaxSize(0.6f)
                        )
                    }
                }
                errorMessage != null && patientList.isEmpty() -> {
                    item(key = "state_error") {
                        ErrorStateContent(
                            errorMessage = errorMessage,
                            onRetry = onRefresh
                        )
                    }
                }
                filteredPatients.isEmpty() -> {
                    item(key = "state_empty") {
                        if (searchQuery.isNotEmpty()) {
                            EmptyStateContent(
                                title = "No matching patients",
                                description = "No patients found matching \"$searchQuery\". Try a different search term.",
                                icon = Icons.Default.SearchOff,
                                actionText = "Clear Search",
                                onActionClick = { searchQuery = "" }
                            )
                        } else {
                            EmptyStateContent(
                                title = "No patients registered yet",
                                description = "You have not registered any patients yet. Click the Add Patient button below to enroll your first patient.",
                                icon = Icons.Default.GroupAdd,
                                actionText = "Add New Patient",
                                onActionClick = onAddPatientClick
                            )
                        }
                    }
                }
                else -> {
                    items(
                        items = filteredPatients,
                        key = { it.username }
                    ) { patient ->
                        PatientCard(
                            patient = patient,
                            onClick = { onPatientClick(patient) }
                        )
                    }
                }
            }
        }
    }
}