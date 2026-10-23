package com.simats.pcos

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TrackView : ComponentActivity() {

    private var caloriesPercentage by mutableFloatStateOf(1200f)
    private var stepsPercentage by mutableFloatStateOf(1500f)
    private var feedbackPercentage by mutableFloatStateOf(800f)
    
    private var barEntries by mutableStateOf<List<BarEntry>>(listOf(
        BarEntry(1f, 2000f),
        BarEntry(2f, 5000f),
        BarEntry(3f, 3000f),
        BarEntry(4f, 7000f),
        BarEntry(5f, 8000f)
    ))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val username = intent.getStringExtra("username") ?: ""

        fetchChartData(username)
        fetchDataFromServerSteps(username)

        setContent {
            TrackViewContent(
                caloriesPercentage = caloriesPercentage,
                stepsPercentage = stepsPercentage,
                feedbackPercentage = feedbackPercentage,
                barEntries = barEntries,
                onLeaderboardClick = {
                    startActivity(Intent(this, LeaderBoard::class.java))
                },
                onCategoryClick = {
                    val intent = Intent(this, ProgressScrfeenActivity::class.java).apply {
                        putExtra("username", username)
                        putExtra("hideButton", true)
                    }
                    startActivity(intent)
                },
                onAssessmentClick = {
                    showAssessmentAlert()
                }
            )
        }
    }

    private fun showAssessmentAlert() {
        AlertDialog.Builder(this)
            .setTitle("Assessment Status")
            .setMessage("Assessment not opened.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun fetchDataFromServerSteps(name: String) {
        val url = IpV4Connection.getUrl("stepsgraph.php")

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                parseDataAndSetupBarChart(response)
            },
            { error ->
                Toast.makeText(applicationContext, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): Map<String, String> {
                return hashMapOf("name" to name)
            }
        }

        requestQueue.add(stringRequest)
    }

    private fun parseDataAndSetupBarChart(response: String) {
        try {
            val cleanResponse = IpV4Connection.cleanJsonResponse(response)
            val jsonObject = JSONObject(cleanResponse)
            if (jsonObject.getBoolean("success")) {
                val days = jsonObject.getJSONArray("days")
                val stepsCounts = jsonObject.getJSONArray("steps_counts")

                val entries = mutableListOf<BarEntry>()
                for (i in 0 until days.length()) {
                    val day = days.getString(i).toFloat()
                    val steps = stepsCounts.getDouble(i).toFloat()
                    entries.add(BarEntry(day, steps))
                }
                barEntries = entries
            } else {
                Toast.makeText(applicationContext, jsonObject.getString("message"), Toast.LENGTH_LONG).show()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error parsing data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchChartData(userName: String) {
        Thread {
            try {
                val url = URL(IpV4Connection.getUrl("percentage.php"))
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.doOutput = true

                val os = conn.outputStream
                val writer = BufferedWriter(java.io.OutputStreamWriter(os, "UTF-8"))
                writer.write("name=" + URLEncoder.encode(userName, "UTF-8"))
                writer.flush()
                writer.close()
                os.close()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inReader = BufferedReader(InputStreamReader(conn.inputStream))
                    var inputLine: String?
                    val response = StringBuffer()

                    while (inReader.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    inReader.close()

                    val cleanResponse = IpV4Connection.cleanJsonResponse(response.toString())
                    updatePieCharts(JSONObject(cleanResponse))
                } else {
                    Log.e("HTTP_ERROR", "Response Code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun updatePieCharts(jsonResponse: JSONObject) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            try {
                if (jsonResponse.getBoolean("success")) {
                    val percentages = jsonResponse.getJSONObject("percentages")
                    caloriesPercentage = percentages.getInt("calories_percentage").toFloat()
                    stepsPercentage = percentages.getInt("steps_percentage").toFloat()
                    feedbackPercentage = percentages.getInt("feedback_percentage").toFloat()
                } else {
                    Toast.makeText(applicationContext, jsonResponse.getString("message"), Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackViewContent(
    caloriesPercentage: Float,
    stepsPercentage: Float,
    feedbackPercentage: Float,
    barEntries: List<BarEntry>,
    onLeaderboardClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onAssessmentClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE91E63) // replace with @color/Another
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 20.dp)
        ) {
            // Today Progress Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Today Progress",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Calories", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "1500Kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PieChartCompose(
                        categoryAmount = caloriesPercentage,
                        totalAmount = 2000f,
                        color = AndroidColor.parseColor("#ffc700")
                    )
                    PieChartCompose(
                        categoryAmount = stepsPercentage,
                        totalAmount = 2500f,
                        color = AndroidColor.parseColor("#0f87ff")
                    )
                    PieChartCompose(
                        categoryAmount = feedbackPercentage,
                        totalAmount = 1800f,
                        color = AndroidColor.parseColor("#846cf8")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = "Calorie", fontSize = 14.sp)
                    Text(text = "Sleep", fontSize = 14.sp)
                    Text(text = "Steps", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Button(
                onClick = onAssessmentClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text(text = "Assessment", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCategoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)) // @color/BoyBlue
            ) {
                Text(text = "View Patient Category", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weight Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(text = "Weight", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "65Kg", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Latest Measurements",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        textAlign = TextAlign.Center
                    )

                    Image(
                        painter = painterResource(id = R.drawable.weight),
                        contentDescription = "Weight Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Leaderboard Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLeaderboardClick)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cupimage),
                    contentDescription = "Leaderboard",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "View Patient Leaderboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Steps Graph
            Text(
                text = "Steps Graph",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 30.dp, top = 20.dp)
            )

            BarChartCompose(
                entries = barEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun PieChartCompose(categoryAmount: Float, totalAmount: Float, color: Int) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(AndroidColor.TRANSPARENT)
                holeRadius = 70f
                transparentCircleRadius = 61f
                setDrawEntryLabels(false)
                setDrawCenterText(true)
                setCenterTextSize(14f)
                setCenterTextColor(AndroidColor.BLACK)
                legend.isEnabled = false
            }
        },
        update = { pieChart ->
            val remainder = if (totalAmount - categoryAmount > 0) totalAmount - categoryAmount else 0f
            val percentageConsumed = (categoryAmount / totalAmount) * 100

            val entries = ArrayList<PieEntry>()
            entries.add(PieEntry(categoryAmount, ""))
            entries.add(PieEntry(remainder, ""))

            val dataSet = PieDataSet(entries, "")
            dataSet.sliceSpace = 3f
            dataSet.selectionShift = 5f
            dataSet.colors = listOf(color, AndroidColor.WHITE)

            val data = PieData(dataSet)
            data.setDrawValues(false)

            pieChart.data = data
            pieChart.setUsePercentValues(true)
            pieChart.centerText = String.format(java.util.Locale.getDefault(), "%.1f%%", percentageConsumed)
            
            pieChart.animateY(1400, Easing.EaseInOutQuad)
            pieChart.invalidate()
        },
        modifier = Modifier.size(80.dp)
    )
}

@Composable
fun BarChartCompose(entries: List<BarEntry>, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                animateY(1000)

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("", "Day 1", "Day 2", "Day 3", "Day 4", "Day 5"))

                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 10000f

                axisRight.isEnabled = false
            }
        },
        update = { barChart ->
            val dataSet = BarDataSet(entries, "Steps")
            dataSet.color = AndroidColor.BLUE // replace with @color/steps

            val barData = BarData(dataSet)
            barChart.data = barData
            barChart.invalidate()
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun TrackViewContentPreview() {
    TrackViewContent(
        caloriesPercentage = 1200f,
        stepsPercentage = 1500f,
        feedbackPercentage = 800f,
        barEntries = listOf(
            BarEntry(1f, 2000f),
            BarEntry(2f, 5000f),
            BarEntry(3f, 3000f),
            BarEntry(4f, 7000f),
            BarEntry(5f, 8000f)
        ),
        onLeaderboardClick = {},
        onCategoryClick = {},
        onAssessmentClick = {}
    )
}
