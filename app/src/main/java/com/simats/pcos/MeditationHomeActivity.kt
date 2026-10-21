package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MeditationHomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""

        setContent {
            MeditationHomeScreen(
                onBack = { finish() },
                onSelectCategory = { category ->
                    val intent = Intent(this, MeditationMusic::class.java).apply {
                        putExtra("CATEGORY", category)
                        putExtra("username", username)
                    }
                    startActivity(intent)
                }
            )
        }
    }
}

data class MeditationCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationHomeScreen(
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val darkPink = Color(0xFFB80257)

    val categories = listOf(
        MeditationCategory(
            title = "Reduce stress",
            subtitle = "Calm your mind & lower cortisol",
            icon = Icons.Default.Spa,
            gradientColors = listOf(Color(0xFFF06292), Color(0xFFE91E63))
        ),
        MeditationCategory(
            title = "Reduce Anxiety",
            subtitle = "Gentle breathing & soothing tones",
            icon = Icons.Default.Air,
            gradientColors = listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
        ),
        MeditationCategory(
            title = "increase happiness",
            subtitle = "Gratitude & inner joy",
            icon = Icons.Default.WbSunny,
            gradientColors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00))
        ),
        MeditationCategory(
            title = "Better sleep",
            subtitle = "Deep sleep frequencies",
            icon = Icons.Default.NightlightRound,
            gradientColors = listOf(Color(0xFF7E57C2), Color(0xFF512DA8))
        ),
        MeditationCategory(
            title = "personal growth",
            subtitle = "Self-compassion & resilience",
            icon = Icons.Default.Eco,
            gradientColors = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
        ),
        MeditationCategory(
            title = "Better Concentration",
            subtitle = "Focus & mental clarity",
            icon = Icons.Default.FilterCenterFocus,
            gradientColors = listOf(Color(0xFF26A69A), Color(0xFF00695C))
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Meditation & Mindfulness",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkPink)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFBF8F9))
                .padding(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Find Your Daily Peace",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Guided audio sessions curated specifically to support endocrine balance and calm nervous tension.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose a Focus Theme",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    MeditationCategoryCard(
                        category = category,
                        onClick = { onSelectCategory(category.title) }
                    )
                }
            }
        }
    }
}

@Composable
fun MeditationCategoryCard(
    category: MeditationCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(category.gradientColors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = category.title.replaceFirstChar { it.uppercase() },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
