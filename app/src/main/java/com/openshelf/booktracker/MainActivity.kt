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

    companion object {
        const val EXTRA_OPEN_STATS = "open_stats"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ runtime notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* granted or denied — either way, continue */ }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Widget tap → open Stats tab directly
        val openStats = intent?.getBooleanExtra(EXTRA_OPEN_STATS, false) == true

        setContent {
            BookTrackerTheme {
                BookTrackerNavGraph(startOnStats = openStats)
            }
        }
    }
}