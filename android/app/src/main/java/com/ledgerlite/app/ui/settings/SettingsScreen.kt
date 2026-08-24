package com.ledgerlite.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledgerlite.app.LedgerLiteApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCategoryManage: () -> Unit) {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val settings = container.settingsRepository
    val scope = rememberCoroutineScope()

    val defaultHome by settings.defaultHome.collectAsStateWithLifecycle(initialValue = "record")
    val currencySymbol by settings.currencySymbol.collectAsStateWithLifecycle(initialValue = "¥")
    val showDecimals by settings.showDecimals.collectAsStateWithLifecycle(initialValue = true)
    val decimalPlaces by settings.decimalPlaces.collectAsStateWithLifecycle(initialValue = 2)
    val recentLimit by settings.recentLimit.collectAsStateWithLifecycle(initialValue = 5)

    var currencyInput by remember(currencySymbol) { mutableStateOf(currencySymbol) }
    var showCustomCurrency by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 默认首页
            Text("默认首页", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("record" to "记账", "ledger" to "流水", "bigitem" to "资产", "stats" to "统计").forEach { (key, label) ->
                    FilterChip(
                        selected = defaultHome == key,
                        onClick = { scope.launch { settings.setDefaultHome(key) } },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // 货币符号：预置常用符号一键选择，另支持自定义
            Text("货币符号", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val presetSymbols = listOf("¥", "$", "€", "£", "₩", "JP¥")
            val isPreset = currencySymbol in presetSymbols
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetSymbols.forEach { symbol ->
                    FilterChip(
                        selected = currencySymbol == symbol,
                        onClick = { scope.launch { settings.setCurrencySymbol(symbol) } },
                        label = { Text(symbol) }
                    )
                }
                FilterChip(
                    selected = !isPreset,
                    onClick = {
                        if (isPreset) {
                            currencyInput = ""
                            showCustomCurrency = true
                        } else {
                            showCustomCurrency = !showCustomCurrency
                        }
                    },
                    label = { Text("自定义") }
                )
            }
            if (!isPreset || showCustomCurrency) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { currencyInput = it.take(3) },
                        label = { Text("自定义符号（1-3 字符）") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val value = currencyInput.trim()
                            if (value.isNotEmpty()) {
                                scope.launch { settings.setCurrencySymbol(value) }
                                showCustomCurrency = false
                            }
                        },
                        enabled = currencyInput.trim().isNotEmpty()
                    ) { Text("保存") }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // 是否显示小数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("金额显示小数", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = showDecimals,
                    onCheckedChange = { v -> scope.launch { settings.setShowDecimals(v) } }
                )
            }

            // 小数位数（仅显示小数时可选）
            if (showDecimals) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("小数位数", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(1 to "1 位", 2 to "2 位").forEach { (n, label) ->
                        FilterChip(
                            selected = decimalPlaces == n,
                            onClick = { scope.launch { settings.setDecimalPlaces(n) } },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // 首页最近记录数量
            Text("首页最近记录数量：$recentLimit", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15).forEach { n ->
                    FilterChip(
                        selected = recentLimit == n,
                        onClick = { scope.launch { settings.setRecentLimit(n) } },
                        label = { Text("$n") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // 分类管理入口
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCategoryManage),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "分类管理",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "进入分类管理",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
