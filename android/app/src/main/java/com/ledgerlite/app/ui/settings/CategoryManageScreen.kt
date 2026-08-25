package com.ledgerlite.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledgerlite.app.LedgerLiteApp
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.ui.components.CategoryIcon
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(onBack: () -> Unit, bottomInset: Dp = 0.dp) {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val categories by container.categoryRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    // pendingDelete 持有待删分类，blockedCount > 0 表示被引用拦截
    var pendingDelete by remember { mutableStateOf<Category?>(null) }
    var blockedCount by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier.padding(bottom = bottomInset)) {
                FloatingActionButton(
                    onClick = { creating = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增分类", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        // 本地维护顺序，拖拽时即时调整；松手写库后由 Flow 回灌
        val ordered = remember(categories) { mutableStateListOf<Category>().apply { addAll(categories) } }
        val lazyListState = rememberLazyListState()
        val scope2 = scope
        val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
            // from/to 是 LazyListItemInfo，取 index
            ordered.add(to.index, ordered.removeAt(from.index))
        }

        // 拖拽结束（dragging 由 true→false）时写回 sortOrder
        LaunchedEffect(reorderState) {
            snapshotFlow { reorderState.isAnyItemDragging }
                .distinctUntilChanged()
                .filter { !it }
                .collect {
                    scope2.launch { container.categoryRepository.reorder(ordered.toList()) }
                }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(ordered, key = { _, c -> c.id }) { index, category ->
                ReorderableItem(reorderState, key = category.id) { isDragging ->
                    CategoryRow(
                        category = category,
                        isDragging = isDragging,
                        dragModifier = Modifier.longPressDraggableHandle(),
                        onEdit = { editing = category },
                        onDelete = {
                            scope.launch {
                                val count = container.expenseRepository.referenceCount(category.id)
                                blockedCount = count
                                pendingDelete = category
                            }
                        }                    )
                }
            }
        }
    }

    // 新建对话框
    if (creating) {
        CategoryEditDialog(
            initialName = "",
            initialColor = defaultColors().first(),
            onDismiss = { creating = false },
            onConfirm = { name, color ->
                scope.launch {
                    container.categoryRepository.create(name, color.toArgbLong())
                    creating = false
                }
            }
        )
    }

    // 编辑对话框
    editing?.let { cat ->
        CategoryEditDialog(
            initialName = cat.name,
            initialColor = Color(cat.color),
            onDismiss = { editing = null },
            onConfirm = { name, color ->
                scope.launch {
                    container.categoryRepository.update(cat.copy(name = name, color = color.toArgbLong()))
                    editing = null
                }
            }
        )
    }

    // 删除二次确认 / 引用拦截
    pendingDelete?.let { cat ->
        val blocked = blockedCount > 0
        AlertDialog(
            onDismissRequest = { pendingDelete = null; blockedCount = 0 },
            title = { Text(if (blocked) "无法删除" else "删除分类？") },
            text = {
                Text(
                    if (blocked) "“${cat.name}” 已被 $blockedCount 条流水引用，不能删除。请先删除或迁移相关流水。"
                    else "删除分类“${cat.name}”？此操作不可撤销。"
                )
            },
            confirmButton = {
                if (blocked) {
                    TextButton(onClick = { pendingDelete = null; blockedCount = 0 }) { Text("知道了") }
                } else {
                    TextButton(onClick = {
                        scope.launch {
                            val deleted = container.categoryRepository.delete(
                                cat.id,
                                referenceCount = { container.expenseRepository.referenceCount(it) }
                            )
                            if (deleted) pendingDelete = null
                        }
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                if (!blocked) {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isDragging: Boolean,
    dragModifier: Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.alpha(0.6f) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(dragModifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val accent = Color(category.color)
            val iconVector = CategoryIcon.vector(category.icon)
            Box(
                modifier = Modifier.size(24.dp).background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(12.dp).background(accent, RoundedCornerShape(3.dp)))
                }
            }
            Spacer(Modifier.size(12.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (category.isDefault) {
                Text(
                    "默认",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initialName: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Color) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    val palette = remember { defaultColors() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "新增分类" else "编辑分类") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(10) },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("颜色", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (selectedColor == color) color else color.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 默认调色板，与种子分类颜色对齐。 */
private fun defaultColors(): List<Color> = listOf(
    Color(0xFFD95F43), Color(0xFF3C6E71), Color(0xFFB57BA6), Color(0xFF8B9D77),
    Color(0xFFE0A458), Color(0xFFC75D5D), Color(0xFF5B7C99), Color(0xFF6D7571),
)

/** Color → ARGB Long（entity 存 Long）。 */
private fun Color.toArgbLong(): Long = this.toArgb().toLong()
