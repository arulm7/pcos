package com.simats.pcos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientAnswersActivity : ComponentActivity() {

    private val answersList = mutableStateListOf<AnswersResponse.AnswerData>()
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawName = intent.getStringExtra("username")
            ?: intent.getStringExtra("patient_name")
            ?: SessionManager.getUsername(this)

        username = rawName.trim()

        if (username.isNotEmpty()) {
            fetchAnswers(username)
        } else {
            errorMessage = "No patient specified"
        }

        setContent {
            PatientAnswersScreen(
                username = username,
                answers = answersList,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBackClick = { finish() },
                onRefresh = { if (username.isNotEmpty()) fetchAnswers(username) }
            )
        }
    }

    private fun fetchAnswers(name: String) {
        isLoading = true
        errorMessage = null
        RestClient.makeAPI().getAnswers(name).enqueue(object : Callback<AnswersResponse> {
            override fun onResponse(call: Call<AnswersResponse>, response: Response<AnswersResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    val body = response.body()
                    val data = body?.data
                    if (data != null && data.isNotEmpty()) {
                        answersList.clear()
                        answersList.addAll(data)
                    } else if (body != null && body.isSuccess && data != null) {
                        answersList.clear()
                        answersList.addAll(data)
                    } else {
                        errorMessage = body?.message ?: "No answers recorded for this patient yet."
                    }
                } else {
                    errorMessage = "Server error code: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<AnswersResponse>, t: Throwable) {
                isLoading = false
                errorMessage = "Network error: ${t.message ?: "Could not reach server"}"
            }
        })
    }

    class AnswersResponse {
        @SerializedName("success")
        var isSuccess: Boolean = false
            private set

        @SerializedName("message")
        var message: String? = null
            private set

        @SerializedName("data")
        var data: List<AnswerData>? = null
            private set

        class AnswerData {
            @SerializedName("question")
            var question: String? = null
                private set

            @SerializedName("answer")
            var answer: String? = null
                private set
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAnswersScreen(
    username: String,
    answers: List<PatientAnswersActivity.AnswersResponse.AnswerData>,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Q&A Assessment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PcosTextPrimary
                        )
                        if (username.isNotEmpty()) {
                            Text(
                                text = "Patient: $username",
                                fontSize = 12.sp,
                                color = PcosTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PcosTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PcosPrimaryPink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = PcosBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && answers.isEmpty() -> {
                    LoadingContent(
                        message = "Loading assessment answers...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null && answers.isEmpty() -> {
                    ErrorStateContent(
                        errorMessage = errorMessage,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                answers.isEmpty() -> {
                    EmptyStateContent(
                        title = "No Assessment Recorded",
                        description = "This patient has not completed a PCOS Risk Assessment questionnaire yet.",
                        icon = Icons.Default.Quiz,
                        actionText = "Refresh",
                        onActionClick = onRefresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = answers,
                            key = { index, item -> "${index}_${item.question}" }
                        ) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(PcosSoftPink),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Q${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PcosDarkPink
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.question ?: "Question",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PcosTextPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.answer ?: "No answer provided",
                                            fontSize = 14.sp,
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
