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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.druanlabs.didicheck.ui.DidIApp
import com.druanlabs.didicheck.ui.Routes
import com.druanlabs.didicheck.ui.theme.DidITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var pendingDeepLink: String? = null

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* preference already stored; worker checks permission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLink = routeFromIntent(intent)
        maybeRequestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            DidITheme {
                var startRoute by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    val needs = withContext(Dispatchers.IO) {
                        (application as DidIApplication).repository.needsOnboarding()
                    }
                    startRoute = if (needs) {
                        Routes.Onboarding
                    } else {
                        pendingDeepLink ?: Routes.Home
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    val route = startRoute
                    if (route == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        DidIApp(startDestination = route)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = routeFromIntent(intent)
    }

    private fun routeFromIntent(intent: Intent?): String? {
        return when (intent?.getStringExtra("open")) {
            "did_i_do_it" -> Routes.DidIDoIt
            "checklist" -> {
                val id = intent.getStringExtra("routineId").orEmpty()
                if (id.isNotBlank()) Routes.checklist(id) else Routes.Home
            }
            else -> null
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
