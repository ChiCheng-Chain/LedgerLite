package com.ledgerlite.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledgerlite.app.LedgerLiteApp
import com.ledgerlite.app.data.repository.BackupData
import com.ledgerlite.app.data.repository.BackupFormatException
import com.ledgerlite.app.data.repository.readTextFromUri
import com.ledgerlite.app.data.repository.writeTextToUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val settings = container.settingsRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAboutDialog by remember { mutableStateOf(false) }

    val defaultHome by settings.defaultHome.collectAsStateWithLifecycle(initialValue = "record")
    val currencySymbol by settings.currencySymbol.collectAsStateWithLifecycle(initialValue = "¥")
    val showDecimals by settings.showDecimals.collectAsStateWithLifecycle(initialValue = true)
    val decimalPlaces by settings.decimalPlaces.collectAsStateWithLifecycle(initialValue = 2)
    val recentLimit by settings.recentLimit.collectAsStateWithLifecycle(initialValue = 5)

    var currencyInput by remember(currencySymbol) { mutableStateOf(currencySymbol) }
    var showCustomCurrency by remember { mutableStateOf(false) }

    // 备份恢复：pendingRestore 非空表示已选好备份文件、待用户确认覆盖
    val backupRepository = container.backupRepository
    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = backupRepository.createBackupJson()
                writeTextToUri(context, uri, text)
                snackbarHostState.showSnackbar("备份完成")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("备份失败：${e.message ?: "未知错误"}")
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                pendingRestore = backupRepository.parseBackupJson(readTextFromUri(context, uri))
            } catch (e: BackupFormatException) {
                snackbarHostState.showSnackbar(e.message ?: "备份文件无效")
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("读取备份文件失败")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // 货币符号：预置常用符号一键选择，另支持自定义
            Text("货币符号", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val presetSymbols = listOf("¥", "$", "€", "£")
            val isPreset = currencySymbol in presetSymbols
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // 备份与恢复
            Text("备份与恢复", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "备份包含全部分类、流水、资产（含回收站）与偏好设置，恢复将覆盖当前全部数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { backupLauncher.launch(backupFileName()) },
                    modifier = Modifier.weight(1f)
                ) { Text("备份") }
                FilledTonalButton(
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("恢复") }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            AboutFooter(
                onStarClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(REPO_URL))
                    try {
                        context.startActivity(intent)
                    } catch (_: android.content.ActivityNotFoundException) { }
                },
                onAuthorClick = { showAboutDialog = true },
                onCopied = { snackbarHostState.showSnackbar("已复制仓库地址") }
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    pendingRestore?.let { backup ->
        RestoreConfirmDialog(
            backup = backup,
            onConfirm = {
                val toRestore = backup
                pendingRestore = null
                scope.launch {
                    try {
                        val result = backupRepository.restore(toRestore)
                        snackbarHostState.showSnackbar(
                            "已恢复：${result.expenseCount} 条流水、${result.categoryCount} 个分类、${result.bigItemCount} 件资产"
                        )
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("恢复失败，当前数据未改动")
                    }
                }
            },
            onDismiss = { pendingRestore = null }
        )
    }
}

/** 备份文件默认名：LedgerLite-backup-yyyyMMdd-HHmmss.json（Locale.US 避免本地化数字）。 */
private fun backupFileName(): String =
    "LedgerLite-backup-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".json"

@Composable
private fun RestoreConfirmDialog(
    backup: BackupData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复备份？") },
        text = {
            Column {
                Text("备份时间：${timeFmt.format(Date(backup.createdAt))}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "包含 ${backup.categories.size} 个分类、${backup.expenseRecords.size} 条流水、${backup.bigItems.size} 件资产。"
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "恢复将覆盖当前全部数据，此操作不可撤销。",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("覆盖恢复", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private const val REPO_URL = "https://github.com/ChiCheng-Chain/LedgerLite"

@Composable
private fun AboutFooter(
    onStarClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onCopied: suspend (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onAuthorClick).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "by ChiCheng",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "如果觉得好用，给我点个 Star",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onStarClick,
            modifier = Modifier.fillMaxWidth(0.7f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Star on GitHub", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        val copyScope = rememberCoroutineScope()
        androidx.compose.material3.OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("LedgerLite 仓库", REPO_URL))
                copyScope.launch { onCopied("已复制仓库地址") }
            },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Copy URL")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "本项目仅供个人学习交流，\n禁止任何形式的商业倒卖与转售。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        title = { Text("关于 LedgerLite") },
        text = {
            Column {
                Text("作者：ChiCheng", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("版本：v0.1.0 (build 1)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("协议：MIT License", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("年份：2026", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    "本项目仅供个人学习交流，禁止任何形式的商业倒卖与转售。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
