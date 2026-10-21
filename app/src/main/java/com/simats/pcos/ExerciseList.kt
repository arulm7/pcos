package com.simats.pcos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ExerciseList : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val providedDay = intent.getStringExtra("day")
        val day = if (!providedDay.isNullOrBlank()) {
            providedDay
        } else {
            val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(java.util.Date())
            if (today == "Sunday") "Monday" else today
        }

        val exercises = getExercisesForDay(day).toList()
        val imageResources = getImagesForExercises(exercises.size, this)

        setContent {
            ExerciseListContent(
                exercises = exercises,
                images = imageResources,
                onExerciseClick = { exerciseName ->
                    val intent = Intent(this, Video1::class.java).apply {
                        putExtra("exerciseName", exerciseName)
                    }
                    startActivity(intent)
                }
            )
        }
    }

    private fun getExercisesForDay(day: String): Array<String> {
        return when (day) {
            "Monday" -> arrayOf("Jumping Jacks", "Assisted Lunges", "Plie Squats", "Walking Squats", "Burpees", "Mountain Climber", "Pendulum Swings", "Mountain Climber (again)", "Flutter Kicks", "Side Lunges", "Backward Lunge", "Crab Kicks", "Deep Squat Hold", "Shoulder Stretch (Left)", "Shoulder Stretch (Right)", "Straight Leg Fire Hydrant (Left)", "Straight Leg Fire Hydrant (Right)", "Glute Kick Back (Left)", "Glute Kick Back (Right)")
            "Tuesday" -> arrayOf("Jumping Jacks", "Assisted Lunges", "Standing Bicycle Crunches", "Russian Twist", "Mountain Climber", "Flutter Kicks", "Leg Raises", "Butt Bridge", "Skipping without Rope", "Standing Bicycle Crunches (again)", "Plank", "Reverse Crunches", "Heel Touch", "Reclined Oblique Twist", "Heels to the Heavens", "Cross Knee Plank", "Cross Arm Crunches", "X-Man Crunch", "Side Crunches (Left/Right)")
            "Wednesday" -> arrayOf("Dynamic Chest", "Triceps Dips", "Push-Ups", "Diagonal Plank", "Incline Push-Ups", "star Crawl", "Arm Scissors", "Triceps Dips (again)", "Push-Ups (again)", "Diagonal Plank (again)", "Incline Push-Ups (again)", "Elbows Back", "Leg Barbell Curl (Left)", "Leg Barbell Curl (Right)", "Jumping Jacks", "Plank Taps", "Elbows Back", "Leg Barbell Curl (Left)", "Leg Barbell Curl (Right)", "Jumping Jacks")
            "Thursday" -> arrayOf("Burpees", "Mountain Climber", "Pendulum Swings", "Mountain Climber (again)", "Side Lunges", "Quick Feet", "Butt Kicks", "Skipping Without Rope", "Side Hop", "Squat Pulses", "Straight Leg Fire Hydrant (Left)", "Straight Leg Fire Hydrant (Right)", "Glute Kick Back (Left)", "Glute Kick Back (Right)", "Fast Spider Lunges", "Side Leg Circuits (Left)", "Side Leg Circuits (Right)", "Deep Squat Hold", "Diagonal Plank", "Reclined Oblique Twist")
            "Friday" -> arrayOf("Plie Squats", "Walking Squats", "Burpees", "Mountain Climber", "Pendulum Swings", "Mountain Climber (again)", "Flutter Kicks", "Side Lunges,Backward Lunge", "Crab Kicks,Skipping without Rope,Side Hop,Quick Feet,Butt Kicks,Squat Pulses,Straight Leg Fire Hydrant (Left)", "Straight Leg Fire Hydrant (Right)", "Glute Kick Back (Left)", "Glute Kick Back (Right)", "Arm Scissors")
            "Saturday" -> arrayOf("Standing Bicycle Crunches", "Russian Twist", "Mountain Climber", "Flutter Kicks", "Leg Raises", "Butt Bridge", "Skipping without Rope", "Standing Bicycle Crunches (again)", "Plank,Reverse Crunches", "Heel Touch", "Reclined Oblique Twist", "Heels to the Heavens", "Cross Knee Plank", "Cross Arm Crunches", "X-Man Crunch", "Side Crunches (Left)", "Side Crunches (Right)", "V-Hold", "Cobra Stretch")
            else -> arrayOf("No exercises for this day")
        }
    }

    private fun getImagesForExercises(count: Int, context: Context): IntArray {
        val imgNames = arrayOf("pic27", "pic28", "yoga3", "yoga5", "yoga6", "yoga4", "pic27", "pic28", "yoga3", "yoga5", "yoga6", "yoga4", "pic27", "pic28", "yoga3", "yoga5", "yoga6", "yoga4", "yoga3", "yoga4", "pic27", "pic28", "pic5", "yoga6")
        val images = IntArray(count)
        for (i in 0 until count) {
            images[i] = context.resources.getIdentifier(imgNames[i % imgNames.size], "drawable", context.packageName)
        }
        return images
    }
}

@Composable
fun ExerciseListContent(
    exercises: List<String>,
    images: IntArray,
    onExerciseClick: (String) -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(id = R.string.your_time_your_space),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEB7DB2), // @color/pink
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(id = R.string.screen_22),
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Get stronger and fitter with us",
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Explore videos for your exercise plan",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEB7DB2), // @color/pink
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(exercises) { index, exercise ->
                    ExerciseCell(
                        exerciseName = exercise,
                        imageRes = if (index < images.size) images[index] else R.drawable.pic27, // fallback image
                        onClick = { onExerciseClick(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseCell(
    exerciseName: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = exerciseName,
            fontSize = 18.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseListContentPreview() {
    val sampleExercises = listOf("Jumping Jacks", "Assisted Lunges", "Plie Squats")
    val sampleImages = intArrayOf(R.drawable.pic27, R.drawable.pic28, R.drawable.yoga3)
    
    ExerciseListContent(
        exercises = sampleExercises,
        images = sampleImages,
        onExerciseClick = {}
    )
}
