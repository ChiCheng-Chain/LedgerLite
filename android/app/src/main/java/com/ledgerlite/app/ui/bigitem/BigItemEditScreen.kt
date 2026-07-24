package com.ledgerlite.app.ui.bigitem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerlite.app.LedgerLiteApp
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.util.AmortizationUtil
import com.ledgerlite.app.util.DateUtil
import com.ledgerlite.app.util.MoneyUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigItemEditScreen(
    itemId: Long?,
    onBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val vm: BigItemViewModel = viewModel(factory = BigItemViewModel.Factory(container.bigItemRepository))

    var name by remember { mutableStateOf("") }
    var amountYuan by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(DateUtil.startOfToday()) }
    var note by remember { mutableStateOf("") }
    var loadedExisting by remember { mutableStateOf(itemId == null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAmountSheet by remember { mutableStateOf(false) }

    // 编辑态：载入现有数据
    LaunchedEffect(itemId) {
        if (itemId != null) {
            vm.getById(itemId)?.let { item ->
                name = item.name
                amountYuan = MoneyUtil.centsToYuan(item.amount, withGrouping = false)
                startDate = item.startDate
                note = item.note
                loadedExisting = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "新增资产" else "编辑资产") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (!loadedExisting) {
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // 总金额：点击弹计算器面板（不用系统输入法）。用 Box 包裹避免 OutlinedTextField 消费点击。
            val amountDisplay = if (amountYuan.isEmpty()) "点击输入金额" else "¥$amountYuan"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAmountSheet = true }
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Column {
                    Text("总金额", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        amountDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (amountYuan.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // 开始日期：点击弹 DatePicker，同样用 Box 包裹
            val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Column {
                    Text("开始日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        dateFmt.format(Date(startDate)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // 实时预览日均成本（按今天 - 开始日期 算）
            val amountCents = MoneyUtil.yuanToCents(amountYuan)
            if (amountCents > 0) {
                val preview = BigItem(
                    id = 0, name = name, amount = amountCents, startDate = startDate,
                    createdAt = 0, updatedAt = 0
                )
                Text(
                    "已使用 ${AmortizationUtil.totalDays(preview)} 天 · 日均 ¥${MoneyUtil.centsToYuan(AmortizationUtil.dailyCost(preview))} · 周均 ¥${MoneyUtil.centsToYuan(AmortizationUtil.weeklyCost(preview))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && amountCents > 0) {
                        if (itemId == null) {
                            vm.create(name, amountCents, startDate, note)
                            onBack()
                        } else {
                            vm.updateExisting(itemId, name, amountCents, startDate, note) {
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = name.isNotBlank() && amountCents > 0,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("保存", color = MaterialTheme.colorScheme.onPrimary)
            }

            if (itemId != null) {
                TextButton(
                    onClick = { vm.endItem(itemId); onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("结束该资产", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }

    // 金额计算器弹窗
    if (showAmountSheet) {
        com.ledgerlite.app.ui.components.AmountInputSheet(
            initial = amountYuan,
            onDismiss = { showAmountSheet = false },
            onConfirm = {
                amountYuan = it
                showAmountSheet = false
            }
        )
    }
}
