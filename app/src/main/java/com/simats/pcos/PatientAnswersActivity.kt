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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientAnswersActivity : ComponentActivity() {

    private val answersList = mutableStateListOf<AnswersResponse.AnswerData>()
    private var isLoading by mutableStateOf(false)
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        username = intent.getStringExtra("username") ?: intent.getStringExtra("patient_name") ?: ""

        if (username.isNotEmpty()) {
            fetchAnswers(username)
        } else {
            Toast.makeText(this, "No patient specified", Toast.LENGTH_SHORT).show()
        }

        setContent {
            PatientAnswersScreen(
                username = username,
                answers = answersList,
                isLoading = isLoading,
                onBackClick = { finish() },
                onRefresh = { if (username.isNotEmpty()) fetchAnswers(username) }
            )
        }
    }

    private fun fetchAnswers(name: String) {
        isLoading = true
        RestClient.makeAPI().getAnswers(name).enqueue(object : Callback<AnswersResponse> {
            override fun onResponse(call: Call<AnswersResponse>, response: Response<AnswersResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.isSuccess) {
                        answersList.clear()
                        body.data?.let { answersList.addAll(it) }
                    } else {
                        Toast.makeText(this@PatientAnswersActivity, body?.message ?: "No answers found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PatientAnswersActivity, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AnswersResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(this@PatientAnswersActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class AnswersResponse {
        var isSuccess: Boolean = false
            private set
        var message: String? = null
            private set
        var data: List<AnswerData>? = null
            private set

        class AnswerData {
            var question: String? = null
                private set
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
                            color = Color(0xFF1E293B)
                        )
                        if (username.isNotEmpty()) {
                            Text(
                                text = "Patient: $username",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFEB7DB2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading && answers.isEmpty()) {
                CircularProgressIndicator(
                    color = Color(0xFFEB7DB2),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (answers.isEmpty()) {
                Text(
                    text = "No assessment answers found for this patient.",
                    color = Color(0xFF64748B),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(answers) { index, item ->
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
                                            .background(Color(0xFFFFF0F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Q${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEB7DB2)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.question ?: "Question",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
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
