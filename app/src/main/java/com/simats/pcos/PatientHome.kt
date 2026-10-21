package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PatientHome : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PatientHomeContent(
                username = username,
                onNavigate = { destination ->
                    val intent = Intent(this, destination)
                    intent.putExtra("username", username)
                    intent.putExtra("patient_name", username)
                    startActivity(intent)
                },
                onLogoutClick = {
                    confirmLogout()
                }
            )
        }
    }

    private fun confirmLogout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("OK") { _, _ ->
                SessionManager.clearSession(this)
                val intent = Intent(this, RollSelection::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeContent(
    username: String,
    onNavigate: (Class<*>) -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .clickable { onNavigate(PatientProfile::class.java) }
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = username,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFFFA2CF)
                    )
                }
            }

            // Record Daily Routine Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigate(TodayProgressPatients::class.java) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA2CF).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFB80257),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Record Daily Progress",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Log steps, calories & feedback",
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFB80257)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of features
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardCard(
                        title = "Progress",
                        imageRes = R.drawable.progress,
                        onClick = { onNavigate(ProgressScrfeenActivity::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Food Menu",
                        imageRes = R.drawable.food,
                        onClick = { onNavigate(FoodMenu::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Track Symptoms",
                        imageRes = R.drawable.track,
                        onClick = { onNavigate(TrackView::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Exercise",
                        imageRes = R.drawable.exercise,
                        onClick = { onNavigate(ExerciseList::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "My Reports",
                        imageRes = R.drawable.pic14,
                        onClick = { onNavigate(MedicalReports::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Meditation",
                        imageRes = R.drawable.meditation,
                        onClick = { onNavigate(MeditationHomeActivity::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Menstrual Calendar",
                        imageRes = R.drawable.calender,
                        onClick = { onNavigate(Calender::class.java) }
                    )
                }
                item {
                    DashboardCard(
                        title = "Community",
                        imageRes = R.drawable.group666,
                        onClick = { onNavigate(LeaderBoard::class.java) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)) // @color/lightWhite
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(70.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardCardPreview() {
    DashboardCard(
        title = "Progress",
        imageRes = R.drawable.progress,
        onClick = {}
    )
}