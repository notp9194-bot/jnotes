package com.notp9194bot.jnotesadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notp9194bot.jnotesadmin.ui.AdminApp
import com.notp9194bot.jnotesadmin.ui.theme.AdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdminTheme {
                AdminApp()
            }
        }
    }
}
