package com.day.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.day.app.navigation.DayNavHost
import com.day.app.navigation.Screen
import com.day.app.ui.theme.DAYTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            DAYTheme {
                val navController = rememberNavController()

                // Check intent extras for widget / notification deep links
                val navigateTaskId = intent?.getLongExtra("EXTRA_NAVIGATE_TASK_ID", -1L) ?: -1L
                val navigateAddTask = intent?.getBooleanExtra("EXTRA_NAVIGATE_ADD_TASK", false) ?: false

                val startDestination = when {
                    navigateTaskId > 0L -> Screen.TaskDetails.createRoute(navigateTaskId)
                    navigateAddTask -> Screen.AddTask.route
                    else -> Screen.Splash.route
                }

                DayNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
