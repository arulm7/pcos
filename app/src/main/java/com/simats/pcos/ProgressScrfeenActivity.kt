package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class ProgressScrfeenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""
        val hideButton = intent.getBooleanExtra("hideButton", false)

        setContent {
            ProgressCategoryScreen(
                username = username,
                hideButton = hideButton,
                onBack = { finish() },
                onNavigateHome = {
                    val homeIntent = Intent(this, PatientHome::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }
}

class ProgressBar : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""
        val hideButton = intent.getBooleanExtra("hideButton", false)

        setContent {
            ProgressCategoryScreen(
                username = username,
                hideButton = hideButton,
                onBack = { finish() },
                onNavigateHome = {
                    val homeIntent = Intent(this, PatientHome::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressCategoryScreen(
    username: String,
    hideButton: Boolean,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mildScore by remember { mutableFloatStateOf(0f) }
    var moderateScore by remember { mutableFloatStateOf(0f) }
    var severeScore by remember { mutableFloatStateOf(0f) }

    val mildColor = Color(0xFF6A01F4)
    val moderateColor = Color(0xFF00BFA5)
    val severeColor = Color(0xFFFF7455)
    val primaryPink = Color(0xFFEB7DB2)
    val darkPink = Color(0xFFB80257)

    fun fetchCategoryScores() {
        isLoading = true
        errorMessage = null
        val url = IpV4Connection.getUrl("category.php")
        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        val data = jsonResponse.getJSONObject("data")
                        mildScore = data.optDouble("mild", 0.0).toFloat()
                        moderateScore = data.optDouble("moderate", 0.0).toFloat()
                        severeScore = data.optDouble("severe", 0.0).toFloat()
                    } else {
                        errorMessage = jsonResponse.optString("message", "Could not fetch category scores.")
                    }
                } catch (e: JSONException) {
                    errorMessage = "Error parsing response: ${e.message}"
                } finally {
                    isLoading = false
                }
            },
            { error ->
                isLoading = false
                errorMessage = "Failed to connect to server. Please check your network."
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf("name" to username)
            }
        }
        Volley.newRequestQueue(context).add(stringRequest)
    }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            fetchCategoryScores()
        } else {
            isLoading = false
            errorMessage = "No patient username provided."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Category Analysis",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
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
                actions = {
                    if (!hideButton) {
                        IconButton(onClick = onNavigateHome) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkPink
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFBF8F9))
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = primaryPink)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing assessment results...", color = Color.Gray, fontSize = 15.sp)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { fetchCategoryScores() },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryPink)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                else -> {
                    val actualTotal = mildScore + moderateScore + severeScore
                    
                    val mildPct = if (actualTotal > 0) (mildScore / actualTotal) * 100f else 0f
                    val modPct = if (actualTotal > 0) (moderateScore / actualTotal) * 100f else 0f
                    val sevPct = if (actualTotal > 0) (severeScore / actualTotal) * 100f else 0f

                    // Dominant category determination
                    val dominantCategory = when {
                        actualTotal == 0f -> "No Data"
                        severeScore >= moderateScore && severeScore >= mildScore && severeScore > 0 -> "Severe"
                        moderateScore >= mildScore && moderateScore > 0 -> "Moderate"
                        mildScore > 0 -> "Mild"
                        else -> "Balanced"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header description
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Patient Risk Breakdown",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (actualTotal == 0f) "No assessment data available yet." else "Based on symptom evaluations and questionnaire answers",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // Donut Chart with Compose Canvas
                                DonutChartComposable(
                                    mildPercent = mildPct,
                                    moderatePercent = modPct,
                                    severePercent = sevPct,
                                    dominantCategory = dominantCategory,
                                    mildColor = mildColor,
                                    moderateColor = moderateColor,
                                    severeColor = severeColor
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Legend
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    LegendItem(label = "Mild", color = mildColor, score = mildScore)
                                    LegendItem(label = "Moderate", color = moderateColor, score = moderateScore)
                                    LegendItem(label = "Severe", color = severeColor, score = severeScore)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category Detail Cards
                        Text(
                            text = "Score Analysis",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        CategoryScoreCard(
                            title = "Mild Category",
                            score = mildScore,
                            percentage = mildPct,
                            color = mildColor,
                            description = "Slight hormonal or metabolic fluctuations manageable through daily habits."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CategoryScoreCard(
                            title = "Moderate Category",
                            score = moderateScore,
                            percentage = modPct,
                            color = moderateColor,
                            description = "Moderate risk indicators observed. Dietary and exercise discipline required."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CategoryScoreCard(
                            title = "Severe Category",
                            score = severeScore,
                            percentage = sevPct,
                            color = severeColor,
                            description = "Higher risk markers. Regular doctor follow-ups and tracking strongly advised."
                        )

                        if (!hideButton) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateHome,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryPink)
                            ) {
                                Text(
                                    text = "Go to Home Dashboard",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartComposable(
    mildPercent: Float,
    moderatePercent: Float,
    severePercent: Float,
    dominantCategory: String,
    mildColor: Color,
    moderateColor: Color,
    severeColor: Color
) {
    val total = mildPercent + moderatePercent + severePercent
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(mildPercent, moderatePercent, severePercent) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth = 36.dp.toPx()
            val canvasSize = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(canvasSize, canvasSize)

            if (total == 0f) {
                // Empty state gray ring
                drawArc(
                    color = Color(0xFFEFEFEF),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                val factor = animationProgress.value
                val mildSweep = (mildPercent / total) * 360f * factor
                val moderateSweep = (moderatePercent / total) * 360f * factor
                val severeSweep = (severePercent / total) * 360f * factor

                var currentAngle = -90f

                // Draw Mild slice
                if (mildSweep > 0) {
                    drawArc(
                        color = mildColor,
                        startAngle = currentAngle,
                        sweepAngle = mildSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += mildSweep
                }

                // Draw Moderate slice
                if (moderateSweep > 0) {
                    drawArc(
                        color = moderateColor,
                        startAngle = currentAngle,
                        sweepAngle = moderateSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += moderateSweep
                }

                // Draw Severe slice
                if (severeSweep > 0) {
                    drawArc(
                        color = severeColor,
                        startAngle = currentAngle,
                        sweepAngle = severeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Center label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Predominant",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = dominantCategory,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = when (dominantCategory) {
                    "Severe" -> severeColor
                    "Moderate" -> moderateColor
                    "Mild" -> mildColor
                    else -> Color(0xFF333333)
                }
            )
            Text(
                text = "Category",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, score: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444444))
            Text(text = "${score.toInt()} pts", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun CategoryScoreCard(
    title: String,
    score: Float,
    percentage: Float,
    color: Color,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                }
                Text(
                    text = "${score.toInt()} pts (${percentage.toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}
