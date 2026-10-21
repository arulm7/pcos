package com.simats.pcos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.Locale

data class FoodItem(
    val name: String,
    val calories: Int
)

class FoodListActivity : ComponentActivity() {

    private var username: String = ""
    private var mealType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        username = intent.getStringExtra("username") ?: ""
        mealType = intent.getStringExtra("type") ?: "Meal"

        val rawFoodData = CsvReader.readCsv(this, "Nutrition.csv") ?: arrayListOf()
        val foodList = rawFoodData.mapNotNull { row ->
            if (row.size >= 2) {
                val cal = row[1].trim().toIntOrNull() ?: 0
                FoodItem(name = row[0].trim(), calories = cal)
            } else null
        }

        setContent {
            FoodListScreen(
                mealType = mealType,
                foodList = foodList,
                onBackClick = { finish() },
                onProceed = { selectedNames, totalCalories ->
                    sendCalorieData(username, mealType, totalCalories)
                    val intent = Intent(this@FoodListActivity, MealDescription::class.java).apply {
                        putStringArrayListExtra("selectedItems", ArrayList(selectedNames))
                        putExtra("username", username)
                        putExtra("totalCalories", totalCalories)
                    }
                    startActivity(intent)
                    finish()
                }
            )
        }
    }

    private fun sendCalorieData(name: String, type: String, calorie: Int) {
        val url = IpV4Connection.getUrl("insertFood.php")
        val queue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response -> Log.d("API Response", response) },
            { error -> Log.e("API Error", error.toString()) }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "name" to name,
                    "type" to type,
                    "calorie" to calorie.toString()
                )
            }
        }
        queue.add(stringRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListScreen(
    mealType: String,
    foodList: List<FoodItem>,
    onBackClick: () -> Unit,
    onProceed: (List<String>, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<FoodItem>() }

    val filteredList = remember(searchQuery, foodList) {
        if (searchQuery.isBlank()) {
            foodList
        } else {
            foodList.filter {
                it.name.lowercase(Locale.getDefault()).contains(searchQuery.lowercase(Locale.getDefault()))
            }
        }
    }

    val totalCalories = remember(selectedItems.size) {
        selectedItems.sumOf { it.calories }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${mealType.replaceFirstChar { it.uppercase() }} Nutrition",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${selectedItems.size} selected",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "$totalCalories kcal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEB7DB2)
                        )
                    }

                    Button(
                        onClick = {
                            onProceed(selectedItems.map { it.name }, totalCalories)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)),
                        shape = RoundedCornerShape(20.dp),
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("Search food items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { item ->
                    val isSelected = selectedItems.any { it.name == item.name }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    selectedItems.removeAll { it.name == item.name }
                                } else {
                                    selectedItems.add(item)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFFF0F6) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFEB7DB2) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = item.name,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.calories} cal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color(0xFFEB7DB2) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}
