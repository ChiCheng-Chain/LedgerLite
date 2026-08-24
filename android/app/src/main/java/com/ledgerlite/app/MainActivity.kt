package com.ledgerlite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledgerlite.app.ui.components.DecimalConfig
import com.ledgerlite.app.ui.components.LocalCurrencySymbol
import com.ledgerlite.app.ui.components.LocalDecimalConfig
import com.ledgerlite.app.ui.navigation.AppRoot
import com.ledgerlite.app.ui.navigation.Routes
import com.ledgerlite.app.ui.theme.LedgerLiteTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = (applicationContext as LedgerLiteApp).container.settingsRepository
        // 默认首页只需冷启动生效：同步取一次初值，DataStore 首读很快且仅在 onCreate 执行
        val startTab = runBlocking {
            settings.defaultHome.first()
        }.takeIf { it in Routes.TAB_ROUTES } ?: "record"
        setContent {
            val showDecimals by settings.showDecimals.collectAsStateWithLifecycle(initialValue = true)
            val decimalPlaces by settings.decimalPlaces.collectAsStateWithLifecycle(initialValue = 2)
            val currencySymbol by settings.currencySymbol.collectAsStateWithLifecycle(initialValue = "¥")
            LedgerLiteTheme {
                CompositionLocalProvider(
                    LocalDecimalConfig provides DecimalConfig(showDecimals, decimalPlaces),
                    LocalCurrencySymbol provides currencySymbol
                ) {
                    AppRoot(startTabRoute = startTab)
                }
            }
        }
    }
}
