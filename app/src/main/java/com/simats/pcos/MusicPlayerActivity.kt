package com.simats.pcos

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.Locale

class MusicPlayerActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingState by mutableStateOf(false)
    private var isPreparedState by mutableStateOf(false)
    private var currentPositionState by mutableIntStateOf(0)
    private var durationState by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val audioUrl = intent.getStringExtra("AUDIO_URL") ?: ""

        setupMediaPlayer(audioUrl)

        setContent {
            MusicPlayerScreen(
                isPlaying = isPlayingState,
                isPrepared = isPreparedState,
                currentPosition = currentPositionState,
                duration = durationState,
                onTogglePlay = { togglePlayback() },
                onSeekTo = { pos -> seekTo(pos) },
                onSkip = { offsetMs -> skip(offsetMs) },
                onBack = { finish() }
            )
        }
    }

    private fun setupMediaPlayer(audioUrl: String) {
        if (audioUrl.isEmpty()) {
            Toast.makeText(this, "Missing audio URL", Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { mp ->
                    isPreparedState = true
                    durationState = mp.duration
                    mp.start()
                    isPlayingState = true
                }
                setOnCompletionListener {
                    isPlayingState = false
                    currentPositionState = 0
                }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@MusicPlayerActivity, "Error playing audio", Toast.LENGTH_SHORT).show()
                    true
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MusicPlayerActivity, "Could not open audio stream", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePlayback() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                isPlayingState = false
            } else {
                mp.start()
                isPlayingState = true
            }
        }
    }

    private fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        currentPositionState = positionMs
    }

    private fun skip(offsetMs: Int) {
        mediaPlayer?.let { mp ->
            val newPos = (mp.currentPosition + offsetMs).coerceIn(0, mp.duration)
            mp.seekTo(newPos)
            currentPositionState = newPos
        }
    }

    override fun onResume() {
        super.onResume()
        // Poll position in a coroutine managed in Composable or lifecycle
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPlayingState = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    internal fun updateCurrentPosition() {
        mediaPlayer?.let { mp ->
            if (isPreparedState && mp.isPlaying) {
                currentPositionState = mp.currentPosition
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    isPlaying: Boolean,
    isPrepared: Boolean,
    currentPosition: Int,
    duration: Int,
    onTogglePlay: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkip: (Int) -> Unit,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as? MusicPlayerActivity
    val darkPink = Color(0xFFB80257)
    val primaryPink = Color(0xFFEB7DB2)

    // Position updater timer
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            activity?.updateCurrentPosition()
            delay(500)
        }
    }

    // Breathing pulse animation for meditation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun formatTime(ms: Int): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Now Playing",
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2E082A), Color(0xFF140316))
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Artwork with breathing pulse
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .scale(if (isPlaying) scale else 1f)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFEB7DB2), Color(0xFF880E4F))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Meditation",
                        tint = Color.White,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            // Status label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Meditation Audio Session",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isPrepared) {
                        if (isPlaying) "Breathe in... Breathe out..." else "Paused"
                    } else "Buffering audio stream...",
                    color = Color(0xFFFFB2D6),
                    fontSize = 14.sp
                )
            }

            // Slider & Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                    onValueChange = { newPos ->
                        onSeekTo(newPos.toInt())
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = primaryPink,
                        activeTrackColor = primaryPink,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    enabled = isPrepared
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatTime(duration),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                IconButton(
                    onClick = { onSkip(-10000) },
                    enabled = isPrepared,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Play / Pause FAB
                Surface(
                    onClick = onTogglePlay,
                    enabled = isPrepared,
                    shape = CircleShape,
                    color = primaryPink,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!isPrepared) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Forward 10s
                IconButton(
                    onClick = { onSkip(10000) },
                    enabled = isPrepared,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
