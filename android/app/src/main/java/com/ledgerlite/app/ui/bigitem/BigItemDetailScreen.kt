package com.ledgerlite.app.ui.bigitem

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerlite.app.LedgerLiteApp
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.domain.model.BigItemStatus
import com.ledgerlite.app.ui.components.AmountText
import com.ledgerlite.app.util.AmortizationUtil
import com.ledgerlite.app.util.DateUtil
import com.ledgerlite.app.util.MoneyUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigItemDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val vm: BigItemViewModel = viewModel(factory = BigItemViewModel.Factory(container.bigItemRepository))
    var item by remember { mutableStateOf<BigItem?>(null) }

    LaunchedEffect(itemId) {
        item = vm.getById(itemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "资产详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(itemId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val current = item
        if (current == null) {
            return@Scaffold
        }
        val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
        val active = current.status == BigItemStatus.active

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("总金额", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AmountText(cents = current.amount, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("开始：${dateFmt.format(Date(current.startDate))}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    if (!active && current.endedAt != null) {
                        Text("结束：${dateFmt.format(Date(current.endedAt))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (current.note.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(current.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 使用成本区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用成本", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    val totalDays = AmortizationUtil.totalDays(current)
                    Text("总使用天数：$totalDays 天", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("日均", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AmountText(cents = AmortizationUtil.dailyCost(current), fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("周均", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AmountText(cents = AmortizationUtil.weeklyCost(current), fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    if (!active) {
                        Spacer(Modifier.height(8.dp))
                        Text("已结束，不计入当前使用成本汇总", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onEdit(itemId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("编辑", color = MaterialTheme.colorScheme.onPrimary)
            }
            if (active) {
                OutlinedButton(
                    onClick = { vm.endItem(itemId); onBack() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("结束该资产", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

