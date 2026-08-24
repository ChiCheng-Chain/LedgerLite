package com.ledgerlite.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/** 全局小数显示配置：show=false 只显示整数；places=1 或 2 控制小数位数。 */
data class DecimalConfig(val show: Boolean, val places: Int) {
    companion object {
        val Default = DecimalConfig(show = true, places = 2)
    }
}

val LocalDecimalConfig = staticCompositionLocalOf { DecimalConfig.Default }

/** 全局货币符号，由 MainActivity 从 SettingsRepository 注入，金额展示组件统一读取。 */
val LocalCurrencySymbol = staticCompositionLocalOf { "¥" }
