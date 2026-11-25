package ru.moneyapp.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.moneyapp.domain.model.Category
import ru.moneyapp.domain.model.TransactionType
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.l10n.label
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.categoryIcon
import ru.moneyapp.ui.theme.categoryTint
import ru.moneyapp.ui.theme.expenseTextColor
import ru.moneyapp.ui.theme.incomeTextColor
import ru.moneyapp.ui.theme.savingsTextColor

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val dbCategories by viewModel.categories.collectAsStateWithLifecycle(emptyList())
    var items by remember { mutableStateOf(dbCategories) }
    LaunchedEffect(dbCategories) { items = dbCategories }

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }
    // v1.10.0: tap is now edit; delete moved to the trash icon on the row.
    var editTarget by remember { mutableStateOf<Category?>(null) }

    // v1.10.0: drag identity + original position. The position is captured from
    // the LIVE list at drag start (not a stale composition index), and every drag
    // closure reads `items` / these states live so reorders never move a wrong row.
    var dragType by remember { mutableStateOf<TransactionType?>(null) }
    var dragName by remember { mutableStateOf<String?>(null) }
    var dragFrom by remember { mutableIntStateOf(-1) }

    val sections = remember(items) {
        listOf(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.SAVINGS
        ).map { type -> type to items.filter { it.type == type } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = L10n.s.catManageTitle,
                style = MoneyTypography.titleLarge,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FilledTonalButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(L10n.s.add, style = MoneyTypography.labelMedium)
            }
        }

        Text(
            text = L10n.s.catDragHint,
            style = MoneyTypography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        sections.forEach { (type, list) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = sectionIcon(type),
                    contentDescription = null,
                    tint = sectionAccent(type),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = sectionTitle(type),
                    style = MoneyTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = sectionAccent(type)
                )
            }
            if (list.isEmpty()) {
                Text(
                    text = L10n.s.catEmptyHint,
                    style = MoneyTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            list.forEach { category ->
                // v1.10.0: composite key — a name may repeat across types
                // (e.g. INCOME and EXPENSE «Прочее») once the PK is (name, type).
                key("${type.name}:${category.name}") {
                    CategoryRow(
                        category = category,
                        // v1.10.0: limits computed from the LIVE list each event —
                        // a captured `index` went stale and let the drag invert.
                        canDragUp = {
                            items.filter { it.type == type }
                                .indexOfFirst { it.name == category.name } > 0
                        },
                        canDragDown = {
                            val section = items.filter { it.type == type }
                            section.indexOfFirst { it.name == category.name } < section.size - 1
                        },
                        onToggle = { visible -> viewModel.toggleVisible(type, category.name, visible) },
                        // v1.10.0: tap opens the edit dialog
                        onTap = { editTarget = category },
                        onDelete = { deleteTarget = category },
                        onDragStart = {
                            dragType = type
                            dragName = category.name
                            // original position, read from the live (unmodified) list
                            dragFrom = items.filter { it.type == type }
                                .indexOfFirst { it.name == category.name }
                        },
                        onDragStep = { delta ->
                            items = moveWithinSection(items, category, type, delta)
                        },
                        onDragEnd = {
                            val t = dragType
                            val name = dragName
                            val from = dragFrom
                            dragType = null
                            dragName = null
                            dragFrom = -1
                            if (t != null && name != null) {
                                val section = items.filter { it.type == t }
                                val to = section.indexOfFirst { it.name == name }
                                if (to in section.indices && to != from) {
                                    viewModel.moveInType(t, from, to)
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, iconKey, type ->
                viewModel.add(name, iconKey, type)
                showAddDialog = false
            }
        )
    }

    editTarget?.let { target ->
        EditCategoryDialog(
            initial = target,
            onDismiss = { editTarget = null },
            onSave = { new ->
                viewModel.edit(target, new)
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(L10n.s.catDeleteTitle) },
            text = {
                Text(L10n.s.catDeleteText(target.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // v1.10.0: delete by (type, name)
                        viewModel.delete(target.type, target.name)
                        deleteTarget = null
                    }
                ) {
                    Text(L10n.s.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(L10n.s.cancel)
                }
            }
        )
    }
}

/**
 * Reorders [category] within its type section by [delta] positions (-1 up, +1 down).
 * Returns the new flat list; no-op if the move is impossible.
 */
private fun moveWithinSection(
    items: List<Category>,
    category: Category,
    type: TransactionType,
    delta: Int
): List<Category> {
    val section = items.filter { it.type == type }
    val fromPos = section.indexOfFirst { it.name == category.name }
    if (fromPos < 0) return items
    val toPos = (fromPos + delta).coerceIn(0, section.size - 1)
    if (toPos == fromPos) return items

    val result = items.toMutableList()
    val cat = result.removeAt(result.indexOfFirst { it.name == category.name && it.type == type })
    val sectionAfter = result.filter { it.type == type }
    val insertAt = if (toPos < sectionAfter.size) {
        result.indexOfFirst { it.name == sectionAfter[toPos].name && it.type == type }
    } else {
        (sectionAfter.maxOfOrNull { result.indexOfFirst { r -> r.name == it.name && r.type == type } } ?: (result.size - 1)) + 1
    }
    result.add(insertAt, cat)
    return result
}

@Composable
private fun CategoryRow(
    category: Category,
    canDragUp: () -> Boolean,
    canDragDown: () -> Boolean,
    onToggle: (Boolean) -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragStep: (Int) -> Unit,
    onDragEnd: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var dragging by remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(density) { 56.dp.toPx() }
    val tint = categoryTint(category.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (dragging) colorScheme.primaryContainer.copy(alpha = 0.4f)
                else colorScheme.surfaceVariant
            )
            .clickable(onClick = onTap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle. v1.10.0: the pointer block is keyed by the composite
        // (type,name); its closures read live snapshot state, so a stale
        // composition can no longer drag the wrong row.
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(56.dp)
                .pointerInput("${category.type.name}:${category.name}") {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragOffset.floatValue = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDragStart()
                        },
                        onDragEnd = {
                            dragging = false
                            onDragEnd()
                        },
                        onDragCancel = {
                            dragging = false
                        },
                        onVerticalDrag = { change, amount ->
                            dragOffset.floatValue += amount
                            // v1.10.0: limits re-evaluated from the live list each
                            // step (a captured boolean froze mid-drag and inverted).
                            while (dragOffset.floatValue > rowHeightPx / 2f && canDragDown()) {
                                dragOffset.floatValue -= rowHeightPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDragStep(1)
                            }
                            while (dragOffset.floatValue < -rowHeightPx / 2f && canDragUp()) {
                                dragOffset.floatValue += rowHeightPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDragStep(-1)
                            }
                            change.consume()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = L10n.s.catMove,
                tint = if (dragging) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Tinted category icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // v1.9.0: the user-chosen icon key wins over the name map
                imageVector = categoryIcon(category.name, category.icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = category.name,
            style = MoneyTypography.bodyLarge,
            fontWeight = if (dragging) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = category.isVisible,
            onCheckedChange = onToggle
        )
        Spacer(modifier = Modifier.width(4.dp))

        // v1.10.0: explicit delete (the row tap now edits)
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = L10n.s.delete,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .padding(6.dp)
                .clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    // v1.9.0: iconKey = the user's picker choice (null = automatic by name)
    onAdd: (name: String, iconKey: String?, type: TransactionType) -> Unit
) {
    val s = L10n.s
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.catNewTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.catName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                // v1.9.0: icon grid replaces the free-text emoji field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.catIcon, style = MoneyTypography.labelMedium)
                    CategoryIconPicker(
                        selected = iconKey,
                        onSelect = { iconKey = it }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        TransactionType.SAVINGS
                    ).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label(), style = MoneyTypography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name.trim(), iconKey, type) },
                enabled = name.isNotBlank()
            ) {
                Text(s.add, style = MoneyTypography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel)
            }
        }
    )
}

/**
 * v1.10.0: edit an existing category — rename, change type, or pick a new icon.
 * The new (name, type) is propagated to linked transactions and budget rows.
 */
@Composable
private fun EditCategoryDialog(
    initial: Category,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    val s = L10n.s
    var name by remember { mutableStateOf(initial.name) }
    var iconKey by remember { mutableStateOf(initial.icon) }
    var type by remember { mutableStateOf(initial.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.catEditTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.catName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.catIcon, style = MoneyTypography.labelMedium)
                    CategoryIconPicker(
                        selected = iconKey,
                        onSelect = { iconKey = it }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        TransactionType.SAVINGS
                    ).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label(), style = MoneyTypography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = s.catRenameHint,
                    style = MoneyTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        onSave(
                            initial.copy(name = trimmed, icon = iconKey, type = type)
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(s.save, style = MoneyTypography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel)
            }
        }
    )
}

@Composable
private fun sectionIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.INCOME -> Icons.Rounded.TrendingUp
    TransactionType.EXPENSE -> Icons.Rounded.TrendingDown
    TransactionType.SAVINGS -> Icons.Rounded.Savings
    TransactionType.SAVINGS_SPEND -> Icons.Rounded.TrendingDown
}

@Composable
private fun sectionTitle(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> L10n.s.catSectionIncome
    TransactionType.EXPENSE -> L10n.s.catSectionExpense
    TransactionType.SAVINGS -> L10n.s.catSectionSavings
    TransactionType.SAVINGS_SPEND -> L10n.s.catSectionExpense
}

@Composable
private fun sectionAccent(type: TransactionType): Color = when (type) {
    TransactionType.INCOME -> incomeTextColor()
    TransactionType.EXPENSE -> expenseTextColor()
    TransactionType.SAVINGS -> savingsTextColor()
    TransactionType.SAVINGS_SPEND -> expenseTextColor()
}
