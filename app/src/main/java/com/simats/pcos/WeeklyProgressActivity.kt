package com.simats.pcos

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class WeeklyProgressActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val patientName = intent.getStringExtra("patient_name")
            ?: intent.getStringExtra("username")
            ?: ""

        setContent {
            WeeklyProgressScreen(
                patientName = patientName,
                onBack = { finish() }
            )
        }
    }
}

data class DayProgressRecord(
    val date: String,
    val day: String,
    val exerciseDuration: String,
    val caloriesTaken: String,
    val todaysFeedback: String,
    val steps: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyProgressScreen(
    patientName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Default dates: last 7 days
    val calEnd = remember { Calendar.getInstance() }
    val calStart = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) } }

    var startDateText by remember { mutableStateOf(sdf.format(calStart.time)) }
    var endDateText by remember { mutableStateOf(sdf.format(calEnd.time)) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val records = remember { mutableStateListOf<DayProgressRecord>() }

    val primaryPink = Color(0xFFEB7DB2)
    val darkPink = Color(0xFFB80257)

    fun fetchWeeklyData(start: String, end: String) {
        if (patientName.isEmpty()) {
            errorMessage = "No patient name provided."
            return
        }
        isLoading = true
        errorMessage = null

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL(IpV4Connection.getUrl("week.php"))
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val postData = "name=$patientName&from_date=$start&to_date=$end"
                OutputStreamWriter(connection.outputStream).use { wr ->
                    wr.write(postData)
                    wr.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val rawResult = BufferedReader(InputStreamReader(connection.inputStream, "utf-8")).use {
                        it.readText()
                    }
                    val cleanResult = IpV4Connection.cleanJsonResponse(rawResult)
                    val json = JSONObject(cleanResult)

                    withContext(Dispatchers.Main) {
                        isLoading = false
                        if (json.optBoolean("success", false)) {
                            val dataArray = json.optJSONArray("data")
                            records.clear()
                            if (dataArray != null) {
                                for (i in 0 until dataArray.length()) {
                                    val item = dataArray.getJSONObject(i)
                                    records.add(
                                        DayProgressRecord(
                                            date = item.optString("date", ""),
                                            day = item.optString("day", "") + " day",
                                            exerciseDuration = item.optString("exercise_duration", "0") + " mins",
                                            caloriesTaken = item.optString("calories_taken", "0") + " Kcal",
                                            todaysFeedback = item.optString("todays_feedback", "0") + " hrs",
                                            steps = item.optString("no_of_steps", "0") + " Steps"
                                        )
                                    )
                                }
                            }
                            if (records.isEmpty()) {
                                errorMessage = "No progress records found for this date range."
                            }
                        } else {
                            errorMessage = json.optString("message", "No records found.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = "Server error code: $responseCode"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Error fetching data: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(patientName) {
        fetchWeeklyData(startDateText, endDateText)
    }

    fun showDatePicker(initialDate: String, onDatePicked: (String) -> Unit) {
        val c = Calendar.getInstance()
        try {
            val parsed = sdf.parse(initialDate)
            if (parsed != null) c.time = parsed
        } catch (_: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDatePicked(sdf.format(chosenCal.time))
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Weekly Progress",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        if (patientName.isNotEmpty()) {
                            Text(
                                text = "Patient: $patientName",
                                fontSize = 12.sp,
                                color = Color(0xFFFFD4E5)
                            )
                        }
                    }
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
        ) {
            // Filter card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Date Range",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // From Date
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showDatePicker(startDateText) { newDate ->
                                        startDateText = newDate
                                    }
                                },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("From", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "From Date",
                                        tint = darkPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = startDateText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF333333)
                                    )
                                }
                            }
                        }

                        // To Date
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showDatePicker(endDateText) { newDate ->
                                        endDateText = newDate
                                    }
                                },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("To", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "To Date",
                                        tint = darkPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = endDateText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF333333)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val c = Calendar.getInstance()
                                endDateText = sdf.format(c.time)
                                c.add(Calendar.DAY_OF_YEAR, -7)
                                startDateText = sdf.format(c.time)
                                fetchWeeklyData(startDateText, endDateText)
                            },
                            label = { Text("Last 7 Days", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                val c = Calendar.getInstance()
                                endDateText = sdf.format(c.time)
                                c.add(Calendar.DAY_OF_YEAR, -14)
                                startDateText = sdf.format(c.time)
                                fetchWeeklyData(startDateText, endDateText)
                            },
                            label = { Text("Last 14 Days", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                val c = Calendar.getInstance()
                                endDateText = sdf.format(c.time)
                                c.add(Calendar.DAY_OF_YEAR, -30)
                                startDateText = sdf.format(c.time)
                                fetchWeeklyData(startDateText, endDateText)
                            },
                            label = { Text("Last 30 Days", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { fetchWeeklyData(startDateText, endDateText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryPink)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Progress Log", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Results Section
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryPink)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
                records.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No records available for the selected dates.", color = Color.Gray)
                    }
                }
                else -> {
                    Text(
                        text = "History (${records.size} days logged)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(records) { record ->
                            DayProgressCard(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayProgressCard(record: DayProgressRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Day and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFEB7DB2), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.day.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF222222)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3D2E2)
                ) {
                    Text(
                        text = record.date,
                        fontSize = 12.sp,
                        color = Color(0xFFB80257),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Steps
                MetricBadge(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    title = "Steps",
                    value = record.steps,
                    tint = Color(0xFF1E88E5)
                )

                // Calories
                MetricBadge(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Calories",
                    value = record.caloriesTaken,
                    tint = Color(0xFFFF5722)
                )

                // Exercise
                MetricBadge(
                    icon = Icons.Default.Timer,
                    title = "Exercise",
                    value = record.exerciseDuration,
                    tint = Color(0xFF4CAF50)
                )

                // Sleep / Feedback
                MetricBadge(
                    icon = Icons.Default.NightsStay,
                    title = "Sleep",
                    value = record.todaysFeedback,
                    tint = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
fun MetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(74.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
