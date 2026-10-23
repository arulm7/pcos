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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MedicalReports : ComponentActivity() {
    private val client = OkHttpClient()
    private var records by mutableStateOf<List<MedicalRecord>>(emptyList())
    private var isLoading by mutableStateOf(true)
    private var username = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        username = intent.getStringExtra("username") ?: ""
        
        setContent {
            MedicalReportsContent(
                records = records,
                isLoading = isLoading,
                onImageClick = { imageUrl ->
                    val intent = Intent(this, ImageViewerActivity::class.java).apply {
                        putExtra("image_url", imageUrl)
                    }
                    startActivity(intent)
                },
                onBack = { finish() },
                onAddReport = {
                    val intent = Intent(this, MedicalReports2::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (username.isNotEmpty()) {
            fetchMedicalRecords(username)
        }
    }

    private fun fetchMedicalRecords(name: String) {
        isLoading = true
        val formBody = FormBody.Builder()
            .add("name", name)
            .build()
            
        val request = Request.Builder()
            .url(IpV4Connection.getUrl("view_reports.php"))
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    isLoading = false
                    Toast.makeText(this@MedicalReports, "Error fetching records", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = response.body?.string() ?: ""
                runOnUiThread { 
                    isLoading = false
                    if (response.isSuccessful) {
                        updateUI(jsonResponse)
                    } else {
                        Toast.makeText(this@MedicalReports, "Server error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateUI(rawJsonResponse: String) {
        try {
            val jsonResponse = IpV4Connection.cleanJsonResponse(rawJsonResponse)
            val jsonObject = JSONObject(jsonResponse)
            
            if (jsonObject.optBoolean("success", true) || jsonObject.has("records")) {
                val jsonArray = jsonObject.optJSONArray("records")
                val newRecords = mutableListOf<MedicalRecord>()
                
                if (jsonArray != null) {
                    for (i in 0 until jsonArray.length()) {
                        val record = jsonArray.getJSONObject(i)
                        val rawPath = record.optString("record_path", "")
                        val fullPath = ImageHelper.normalizeImageUrl(rawPath) ?: ""
                        if (fullPath.isNotEmpty()) {
                            newRecords.add(MedicalRecord(fullPath))
                        }
                    }
                }
                records = newRecords
            } else {
                Toast.makeText(this, jsonObject.optString("message", "No records found"), Toast.LENGTH_SHORT).show()
                records = emptyList()
            }
        } catch (_: JSONException) {
            Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show()
            records = emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalReportsContent(
    records: List<MedicalRecord>,
    isLoading: Boolean,
    onImageClick: (String) -> Unit,
    onBack: () -> Unit,
    onAddReport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Reports",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB80257) // Dark Pink
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReport,
                containerColor = Color(0xFFEB7DB2), // Pink
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload Report")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBF8F9))
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEB7DB2))
                }
            } else if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No medical reports found.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(records) { record ->
                        MedicalRecordItem(
                            imageUrl = record.recordPath,
                            onClick = { onImageClick(record.recordPath) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalRecordItem(
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .placeholder(R.drawable.pic1)
                .error(R.drawable.pic1)
                .build(),
            contentDescription = "Medical Report",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
