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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.viewinterop.AndroidView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.json.JSONArray
import org.json.JSONObject

class LeaderBoard : ComponentActivity() {

    private val entries = mutableStateListOf<LeaderboardEntry>()
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLeaderboardData()

        setContent {
            LeaderboardScreen(
                entries = entries,
                isLoading = isLoading,
                onBackClick = { finish() },
                onRefresh = { loadLeaderboardData() }
            )
        }
    }

    private fun loadLeaderboardData() {
        isLoading = true
        val url = IpV4Connection.getUrl("leaderboard.php")
        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                isLoading = false
                try {
                    val topScores: JSONArray = response.getJSONArray("top_scores")
                    entries.clear()
                    for (i in 0 until topScores.length()) {
                        val score: JSONObject = topScores.getJSONObject(i)
                        val name: String = score.optString("Name", "Anonymous")
                        val totalScore: Int = score.optInt("Totalscore", 0)
                        val rawImg = score.optString("ProfileImage", "")
                        val profileImage: String = ImageHelper.normalizeImageUrl(rawImg) ?: ""
                        entries.add(LeaderboardEntry(name, totalScore, profileImage))
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing leaderboard data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                isLoading = false
                Toast.makeText(this, "Failed to load leaderboard: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
    }
}

class LeaderBoard2 : ComponentActivity() {
    private val entries = mutableStateListOf<LeaderboardEntry>()
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadLeaderboardData()

        setContent {
            LeaderboardScreen(
                entries = entries,
                isLoading = isLoading,
                onBackClick = { finish() },
                onRefresh = { loadLeaderboardData() }
            )
        }
    }

    private fun loadLeaderboardData() {
        isLoading = true
        val url = IpV4Connection.getUrl("leaderboard.php")
        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                isLoading = false
                try {
                    val topScores: JSONArray = response.getJSONArray("top_scores")
                    entries.clear()
                    for (i in 0 until topScores.length()) {
                        val score: JSONObject = topScores.getJSONObject(i)
                        val name: String = score.optString("Name", "Anonymous")
                        val totalScore: Int = score.optInt("Totalscore", 0)
                        val rawImg = score.optString("ProfileImage", "")
                        val profileImage: String = ImageHelper.normalizeImageUrl(rawImg) ?: ""
                        entries.add(LeaderboardEntry(name, totalScore, profileImage))
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing leaderboard data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                isLoading = false
                Toast.makeText(this, "Failed to load leaderboard: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Community Leaderboard",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading && entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFEB7DB2))
                }
            } else if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No leaderboard entries found.",
                        color = Color(0xFF64748B),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(entries) { index, entry ->
                        LeaderboardCard(rank = index + 1, entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(rank: Int, entry: LeaderboardEntry) {
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFFE2E8F0)
    }

    val rankTextColor = when (rank) {
        1, 2, 3 -> Color.White
        else -> Color(0xFF475569)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rankBadgeColor),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Bold,
                        color = rankTextColor,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // User Avatar
            ProfileAvatar(
                imageUrl = entry.profileImage,
                name = entry.name,
                size = 44.dp,
                borderColor = Color(0xFFEB7DB2)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // User Name
            Text(
                text = entry.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B),
                modifier = Modifier.weight(1f)
            )

            // User Score
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF0F5)
            ) {
                Text(
                    text = "${entry.totalScore} pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFEB7DB2),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
