package com.druanlabs.didicheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.druanlabs.didicheck.ui.DidIApp
import com.druanlabs.didicheck.ui.Routes
import com.druanlabs.didicheck.ui.theme.DidITheme

class MainActivity : ComponentActivity() {
    private val startRoute = mutableStateOf(Routes.Home)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* preference already stored; worker checks permission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startRoute.value = routeFromIntent(intent)
        maybeRequestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            DidITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DidIApp(startDestination = startRoute.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startRoute.value = routeFromIntent(intent)
    }

    private fun routeFromIntent(intent: Intent?): String {
        return when (intent?.getStringExtra("open")) {
            "did_i_do_it" -> Routes.DidIDoIt
            "checklist" -> {
                val id = intent.getStringExtra("routineId").orEmpty()
                if (id.isNotBlank()) Routes.checklist(id) else Routes.Home
            }
            else -> Routes.Home
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
