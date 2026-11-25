package ru.moneyapp.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.moneyapp.domain.model.*
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.l10n.label
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.expenseTextColor
import ru.moneyapp.ui.theme.incomeTextColor
import ru.moneyapp.ui.theme.savingsColor
import ru.moneyapp.ui.theme.savingsTextColor
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle(emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var showContributionDialog by remember { mutableStateOf<SavingsGoalState?>(null) }
    var showEditDialog by remember { mutableStateOf<SavingsGoalState?>(null) }

    val formatter = remember { DecimalFormat("#,###") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (goals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(L10n.s.goalEmptyTitle, style = MoneyTypography.titleMedium)
                        Text(
                            L10n.s.goalEmptyHint,
                            style = MoneyTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(goals) { goalState ->
                GoalCard(
                    goalState = goalState,
                    formatter = formatter,
                    dateFormatter = dateFormatter,
                    onAddContribution = { showContributionDialog = goalState },
                    onEdit = { showEditDialog = goalState },
                    onDelete = { viewModel.deleteGoal(goalState.goal) }
                )
            }
        }

        item {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(L10n.s.goalCreateBtn, style = MoneyTypography.labelLarge)
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Create goal dialog
    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { goal ->
                viewModel.createGoal(goal)
                showCreateDialog = false
            }
        )
    }

    // Edit goal dialog — same form, pre-filled, saves via updateGoal
    showEditDialog?.let { goalState ->
        CreateGoalDialog(
            initial = goalState.goal,
            onDismiss = { showEditDialog = null },
            onCreate = { goal ->
                viewModel.updateGoal(goal)
                showEditDialog = null
            }
        )
    }

    // Contribution dialog
    showContributionDialog?.let { goalState ->
        ContributionDialog(
            goalState = goalState,
            formatter = formatter,
            onDismiss = { showContributionDialog = null },
            onContribute = { amountKopecks ->
                viewModel.addContribution(goalState.goal.id, amountKopecks, goalState.goal.name)
                showContributionDialog = null
            }
        )
    }
}

@Composable
private fun GoalCard(
    goalState: SavingsGoalState,
    formatter: DecimalFormat,
    dateFormatter: DateTimeFormatter,
    onAddContribution: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = goalState.goal
    val statusColor = when (goalState.status) {
        GoalStatus.COMPLETED -> incomeTextColor()
        GoalStatus.IN_PROGRESS -> savingsColor()
        GoalStatus.OVERDUE -> expenseTextColor()
    }
    val statusIcon: ImageVector = when (goalState.status) {
        GoalStatus.COMPLETED -> Icons.Rounded.Check
        GoalStatus.IN_PROGRESS -> Icons.Rounded.HourglassTop
        GoalStatus.OVERDUE -> Icons.Rounded.Error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = goal.name,
                        style = MoneyTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = goal.description,
                        style = MoneyTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        // v1.9.0: localized status label (follows the in-app language)
                        text = goalState.status.label(),
                        style = MoneyTypography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatter.format(goalState.currentAmountRubles)} ₽",
                        style = MoneyTypography.amountMedium,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${formatter.format(goalState.goal.targetAmountRubles)} ₽",
                        style = MoneyTypography.amountSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                LinearProgressIndicator(
                    progress = { goalState.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    gapSize = 0.dp
                )
                Text(
                    text = L10n.s.goalDone(String.format("%.1f", goalState.progressPercent * 100)),
                    style = MoneyTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.s.goalRemaining(formatter.format(goalState.remainingAmountRubles)),
                    style = MoneyTypography.labelSmall,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = L10n.s.goalDeadline(LocalDate.ofEpochDay(goal.deadline).format(dateFormatter)),
                    style = MoneyTypography.labelSmall,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Recommended monthly
            Text(
                text = L10n.s.goalRecommended(formatter.format(goalState.recommendedMonthlyRubles)),
                style = MoneyTypography.labelSmall,
                color = savingsTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onAddContribution,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.s.goalContribute, style = MoneyTypography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.s.edit, style = MoneyTypography.labelMedium)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = L10n.s.goalDeleteCd,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateGoalDialog(
    initial: SavingsGoal? = null,
    onDismiss: () -> Unit,
    onCreate: (SavingsGoal) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var targetAmount by remember {
        mutableStateOf(initial?.targetAmountKopecks?.let { kopecks ->
            val rubles = kopecks / 100.0
            if (rubles == rubles.toLong().toDouble()) rubles.toLong().toString() else rubles.toString()
        } ?: "")
    }
    var deadline by remember {
        mutableStateOf(initial?.let {
            LocalDate.ofEpochDay(it.deadline).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) L10n.s.goalEditTitle else L10n.s.goalNewTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(L10n.s.goalName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(L10n.s.goalDescription) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it.filter { c -> c.isDigit() } },
                    label = { Text(L10n.s.goalTarget) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text(L10n.s.goalDeadlineField) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val edit = initial
                        val goal = if (edit != null) {
                            edit.copy(
                                name = name,
                                description = description,
                                targetAmountKopecks = ((targetAmount.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                                deadline = LocalDate.parse(deadline).toEpochDay()
                            )
                        } else {
                            SavingsGoal(
                                name = name,
                                description = description,
                                targetAmountKopecks = ((targetAmount.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                                deadline = LocalDate.parse(deadline).toEpochDay()
                            )
                        }
                        onCreate(goal)
                    } catch (e: Exception) {
                        // Invalid date format
                    }
                },
                enabled = name.isNotBlank() && targetAmount.isNotBlank() && deadline.isNotBlank()
            ) {
                Text(if (initial != null) L10n.s.save else L10n.s.create)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.s.cancel)
            }
        }
    )
}

@Composable
private fun ContributionDialog(
    goalState: SavingsGoalState,
    formatter: DecimalFormat,
    onDismiss: () -> Unit,
    onContribute: (Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.s.goalContributionTitle(goalState.goal.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(L10n.s.goalSaved(formatter.format(goalState.currentAmountRubles)), style = MoneyTypography.bodyMedium)
                Text(L10n.s.goalRemaining(formatter.format(goalState.remainingAmountRubles)), style = MoneyTypography.bodyMedium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text(L10n.s.goalContributionAmount) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountKopecks = (amount.toDoubleOrNull() ?: 0.0) * 100
                    if (amountKopecks > 0) onContribute(amountKopecks.toLong())
                },
                enabled = amount.isNotBlank()
            ) {
                Text(L10n.s.goalContribute)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.s.cancel)
            }
        }
    )
}
