package com.openshelf.booktracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.openshelf.booktracker.ui.navigation.BookTrackerNavGraph
import com.openshelf.booktracker.ui.theme.BookTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ runtime permission for notifications.
        // If denied, the app still works — only the daily reminder won't fire.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* granted or denied — both acceptable */ }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            BookTrackerTheme {
                BookTrackerNavGraph()
            }
        }
    }
}