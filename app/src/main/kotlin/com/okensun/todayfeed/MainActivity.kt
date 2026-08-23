package com.okensun.todayfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.okensun.todayfeed.core.designsystem.TodayFeedTheme
import com.okensun.todayfeed.navigation.TodayFeedApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TodayFeedTheme {
                TodayFeedApp()
            }
        }
    }
}
