package com.simats.pcos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class LaunchScreen : ComponentActivity() {

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Persistent login check
        if (SessionManager.isLoggedIn(this)) {
            val role = SessionManager.getRole(this)
            val username = SessionManager.getUsername(this)
            when (role) {
                SessionManager.ROLE_PATIENT -> {
                    val intent = Intent(this, PatientHome::class.java).apply {
                        putExtra("username", username)
                        putExtra("patient_name", username)
                    }
                    startActivity(intent)
                    finish()
                    return
                }
                SessionManager.ROLE_DOCTOR -> {
                    val intent = Intent(this, DoctorHomepage::class.java).apply {
                        putExtra("username", username)
                        putExtra("doctor_id", username)
                    }
                    startActivity(intent)
                    finish()
                    return
                }
                SessionManager.ROLE_ADMIN -> {
                    val intent = Intent(this, AddDoctorsActivity::class.java)
                    startActivity(intent)
                    finish()
                    return
                }
            }
        }

        setContent {
            LaunchScreenContent(
                onNavigateNext = {
                    val intent = Intent(this@LaunchScreen, RollSelection::class.java)
                    startActivity(intent)
                }
            )
        }
        checkAppUpdate(this)
    }

    private fun checkAppUpdate(context: Context) {
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                Toast.makeText(context, "Update Available", Toast.LENGTH_SHORT).show()
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }.addOnFailureListener { e ->
            // Log failure quietly without disturbing user experience during debug/offline launches
            android.util.Log.w("LaunchScreen", "App update check failed: ${e.message}")
        }
    }
}

@Composable
fun LaunchScreenContent(onNavigateNext: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.pcos),
                contentDescription = "PCOS",
                modifier = Modifier.padding(bottom = 35.dp)
            )
            
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(300.dp)
                    .padding(bottom = 100.dp)
            )
            
            Button(
                onClick = onNavigateNext,
                modifier = Modifier.size(width = 250.dp, height = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB7DB2)) // @color/pink
            ) {
                Text(text = "Go With The Flow", color = Color.White)
            }
        }
    }
}