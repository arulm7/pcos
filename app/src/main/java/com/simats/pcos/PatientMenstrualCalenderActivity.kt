package com.simats.pcos

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PatientMenstrualCalenderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val patientName = intent.getStringExtra("patient_name")
            ?: intent.getStringExtra("username")
            ?: ""

        setContent {
            DoctorPatientCalendarScreen(
                patientName = patientName,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientCalendarScreen(
    patientName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val client = remember { OkHttpClient() }
    val highlightedDays = remember { mutableStateListOf<CalendarDay>() }
    var calendarViewRef by remember { mutableStateOf<MaterialCalendarView?>(null) }

    val primaryPink = Color(0xFFEB7DB2)
    val darkPink = Color(0xFFB80257)

    fun fetchPatientDates() {
        isLoading = true
        errorMessage = null

        val body = FormBody.Builder().add("name", patientName).build()
        val request = Request.Builder()
            .url(IpV4Connection.getUrl("calenderD.php"))
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? ComponentActivity)?.runOnUiThread {
                    isLoading = false
                    errorMessage = "Failed to load patient dates: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val rawBody = response.body()?.string() ?: ""
                val clean = IpV4Connection.cleanJsonResponse(rawBody)
                (context as? ComponentActivity)?.runOnUiThread {
                    isLoading = false
                    try {
                        val jsonObject = JSONObject(clean)
                        val datesArray = jsonObject.optJSONArray("dates")
                        highlightedDays.clear()
                        if (datesArray != null) {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            for (i in 0 until datesArray.length()) {
                                val dStr = datesArray.getString(i)
                                try {
                                    val ld = LocalDate.parse(dStr, formatter)
                                    val cd = CalendarDay.from(ld.year, ld.monthValue - 1, ld.dayOfMonth)
                                    highlightedDays.add(cd)
                                } catch (_: Exception) {}
                            }
                        }
                        // Update decorator
                        calendarViewRef?.let { cv ->
                            val color = context.resources.getColor(R.color.highlight_color, context.theme)
                            cv.removeDecorators()
                            cv.addDecorator(EventDecorator(color, highlightedDays))
                            cv.invalidateDecorators()
                        }
                    } catch (e: JSONException) {
                        errorMessage = "Error parsing patient dates: ${e.message}"
                    }
                }
            }
        })
    }

    LaunchedEffect(patientName) {
        fetchPatientDates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Patient Menstrual Log",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Patient Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = darkPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cycle History Overview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF222222)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Recorded menstrual cycle days for patient $patientName. Dates highlighted in pink represent logged cycle dates.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MaterialCalendarView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                calendarViewRef = this
                            }
                        },
                        update = { cv ->
                            calendarViewRef = cv
                            val color = cv.context.resources.getColor(R.color.highlight_color, cv.context.theme)
                            cv.removeDecorators()
                            cv.addDecorator(EventDecorator(color, highlightedDays))
                            cv.invalidateDecorators()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Days Logged Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3D2E2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Logged Cycle Days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = darkPink
                    )
                    Text(
                        text = "${highlightedDays.size} Days",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkPink
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
