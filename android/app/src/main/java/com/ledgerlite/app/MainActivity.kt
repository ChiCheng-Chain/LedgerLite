package com.ledgerlite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ledgerlite.app.ui.navigation.AppRoot
import com.ledgerlite.app.ui.theme.LedgerLiteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LedgerLiteTheme {
                AppRoot()
            }
        }
    }
}
