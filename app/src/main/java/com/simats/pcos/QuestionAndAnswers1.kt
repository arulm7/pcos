package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QuestionAndAnswers1 : ComponentActivity() {

    private var username: String = ""
    private var mild: Int = 0
    private var moderate: Int = 0
    private var severe: Int = 0
    private val storeAnswers = mutableListOf<String>()

    class StoreAnswers(
        val username: String,
        val answers: List<String>
    )

    data class OptionItem(val text: String)
    data class QuestionItem(val text: String, val options: List<OptionItem>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        username = intent.getStringExtra("username") ?: ""

        setContent {
            AssessmentScreen(
                onBack = { finish() },
                onSubmitResults = { answersList, m, mod, sev ->
                    storeAnswers.clear()
                    storeAnswers.addAll(answersList)
                    mild = m
                    moderate = mod
                    severe = sev
                    sendResults()
                }
            )
        }
    }

    private fun sendResults() {
        val url = IpV4Connection.getBaseUrl() + "ans.php"
        val queue = Volley.newRequestQueue(this)
        val postData = JSONObject().apply {
            try {
                put("name", username)
                put("mild", mild)
                put("moderate", moderate)
                put("severe", severe)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, postData,
            { response ->
                try {
                    if (response.has("success")) {
                        navigateToResultActivity()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed: " + response.optString("error", "Unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to send results", Toast.LENGTH_LONG).show()
                error.printStackTrace()
            }
        )

        queue.add(jsonObjectRequest)
    }

    private fun navigateToResultActivity() {
        val ans = StoreAnswers(username, storeAnswers)

        RestClient.makeAPI().storeAnswer(ans).enqueue(object : Callback<CommonResponse> {
            override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val intent = Intent(this@QuestionAndAnswers1, Gifloader::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Fallback to Gifloader even if storeAnswer message is mild
                    val intent = Intent(this@QuestionAndAnswers1, Gifloader::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                Toast.makeText(this@QuestionAndAnswers1, t.message ?: "Network error", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@QuestionAndAnswers1, Gifloader::class.java).apply {
                    putExtra("username", username)
                }
                startActivity(intent)
                finish()
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentScreen(
    onBack: () -> Unit,
    onSubmitResults: (answersList: List<String>, mild: Int, moderate: Int, severe: Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val questions = remember { mutableStateListOf<QuestionAndAnswers1.QuestionItem>() }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    val accumulatedAnswers = remember { mutableStateListOf<String>() }
    var mildScore by remember { mutableIntStateOf(0) }
    var moderateScore by remember { mutableIntStateOf(0) }
    var severeScore by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    val primaryPink = Color(0xFFEB7DB2)
    val darkPink = Color(0xFFB80257)

    fun fetchQuestions() {
        isLoading = true
        errorMessage = null
        val url = IpV4Connection.getBaseUrl() + "qns.php"
        val queue = Volley.newRequestQueue(context)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val rawQuestions: JSONArray = response.getJSONArray("questions")
                    questions.clear()
                    for (i in 0 until rawQuestions.length()) {
                        val qObj = rawQuestions.getJSONObject(i)
                        val qText = qObj.getString("question")
                        val optArray = qObj.getJSONArray("options")
                        val optList = mutableListOf<QuestionAndAnswers1.OptionItem>()
                        for (j in 0 until optArray.length()) {
                            val optObj = optArray.getJSONObject(j)
                            optList.add(QuestionAndAnswers1.OptionItem(optObj.getString("option_text")))
                        }
                        questions.add(QuestionAndAnswers1.QuestionItem(qText, optList))
                    }
                    isLoading = false
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Error parsing questions: ${e.message}"
                }
            },
            { error ->
                isLoading = false
                errorMessage = "Failed to fetch questions. Check network or server connection."
            }
        )
        queue.add(jsonObjectRequest)
    }

    LaunchedEffect(Unit) {
        fetchQuestions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PCOS Risk Assessment",
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
                        Text(
                            text = "Loading questions...",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
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
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { fetchQuestions() },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryPink)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                questions.isNotEmpty() && currentQuestionIndex < questions.size -> {
                    val currentQuestion = questions[currentQuestionIndex]
                    val totalCount = questions.size
                    val progress = (currentQuestionIndex + 1).toFloat() / totalCount

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp)
                    ) {
                        // Progress bar section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Question ${currentQuestionIndex + 1} of $totalCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = darkPink
                                    )
                                    Text(
                                        text = "${((progress) * 100).toInt()}%",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = primaryPink,
                                    trackColor = Color(0xFFF3D2E2)
                                )
                            }
                        }

                        // Question title card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFFFF0F6), Color.White)
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = currentQuestion.text,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF222222),
                                    lineHeight = 26.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Select an option:",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )

                        // Options list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(currentQuestion.options) { index, option ->
                                val isSelected = selectedOptionIndex == index
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedOptionIndex = index
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primaryPink else Color(0xFFE5E5E5)
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFFF5F9) else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    color = if (isSelected) primaryPink else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(Color.White, CircleShape)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Text(
                                            text = option.text,
                                            fontSize = 16.sp,
                                            color = if (isSelected) darkPink else Color(0xFF333333),
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Navigation Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedOptionIndex != null && !isSubmitting) {
                                    val chosenIndex = selectedOptionIndex!!
                                    val chosenText = currentQuestion.options[chosenIndex].text
                                    accumulatedAnswers.add(chosenText)

                                    // Score calculation matching original algorithm:
                                    // index 0..2: mild, index 3..4: moderate, index >= 5: severe
                                    if (chosenIndex < 3) {
                                        mildScore += 1
                                    } else if (chosenIndex < 5) {
                                        moderateScore += 1
                                    } else {
                                        severeScore += 1
                                    }

                                    if (currentQuestionIndex < questions.size - 1) {
                                        currentQuestionIndex++
                                        selectedOptionIndex = null
                                    } else {
                                        isSubmitting = true
                                        onSubmitResults(
                                            accumulatedAnswers.toList(),
                                            mildScore,
                                            moderateScore,
                                            severeScore
                                        )
                                    }
                                }
                            },
                            enabled = selectedOptionIndex != null && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryPink,
                                disabledContainerColor = Color(0xFFF3D2E2)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (currentQuestionIndex == questions.size - 1) "Submit Assessment" else "Next Question",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
