package ru.moneyapp.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import ru.moneyapp.BuildConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.moneyapp.domain.model.BudgetTemplate
import ru.moneyapp.domain.model.BudgetType
import ru.moneyapp.domain.model.Category
import ru.moneyapp.domain.model.DefaultCategories
import ru.moneyapp.domain.model.TransactionType
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.expenseTextColor
import ru.moneyapp.ui.theme.incomeTextColor
import ru.moneyapp.ui.theme.savingsTextColor

private const val XLSX_MIME =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val username by viewModel.username.collectAsStateWithLifecycle("")
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle("₽")
    val googleConnected by viewModel.googleConnected.collectAsStateWithLifecycle(false)
    val spreadsheetId by viewModel.spreadsheetId.collectAsStateWithLifecycle("")
    val clientId by viewModel.googleClientId.collectAsStateWithLifecycle("")
    val spreadsheetLink by viewModel.spreadsheetLink.collectAsStateWithLifecycle("")
    val reminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle(false)
    val reminderHour by viewModel.dailyReminderHour.collectAsStateWithLifecycle(21)
    val reminderMinute by viewModel.dailyReminderMinute.collectAsStateWithLifecycle(0)
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle("system")
    val language by viewModel.language.collectAsStateWithLifecycle("ru") // v1.9.0

    // v1.6.0: monthly budget
    val monthlyCapKopecks by viewModel.monthlyCapKopecks.collectAsStateWithLifecycle(0L)
    val capLinkedIncome by viewModel.capLinkedIncome.collectAsStateWithLifecycle(false)
    val capIncomePct by viewModel.capIncomePct.collectAsStateWithLifecycle(100)
    val budgetCopyPrevMonth by viewModel.budgetCopyPrevMonth.collectAsStateWithLifecycle(false)
    val budgetTemplates by viewModel.budgetTemplates.collectAsStateWithLifecycle(emptyList())
    val lastCrash by viewModel.lastCrash.collectAsStateWithLifecycle("")

    // Text inputs are local state, synced to the persistent values (blank = off)
    var capText by remember { mutableStateOf(kopecksToText(monthlyCapKopecks)) }
    LaunchedEffect(monthlyCapKopecks) { capText = kopecksToText(monthlyCapKopecks) }
    var pctText by remember { mutableStateOf(capIncomePct.toString()) }
    LaunchedEffect(capIncomePct) { pctText = capIncomePct.toString() }
    var showTemplates by remember { mutableStateOf(false) }
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle(false)
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val oauthIntent by viewModel.oauthLaunch.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var oauthExpanded by remember { mutableStateOf(clientId.isNotBlank()) }

    // SAF launchers
    val createExcelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri -> uri?.let { viewModel.exportExcel(it) } }

    val openExcelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importExcelPick(it) } }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackupPick(it) } }

    val oauthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onOauthResult(result)
    }

    // Launch the OAuth web view when the ViewModel asks for it
    LaunchedEffect(oauthIntent) {
        val intent = oauthIntent
        if (intent != null) {
            oauthLauncher.launch(intent)
            viewModel.clearOauthLaunch()
        }
    }

    // One-shot UI events → snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SettingsViewModel.SettingsEvent.Success -> event.message
                is SettingsViewModel.SettingsEvent.Error -> event.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(L10n.s.setReminderTime) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setReminderTime(state.hour, state.minute)
                        showTimePicker = false
                    }
                ) { Text(L10n.s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(L10n.s.cancel) }
            }
        )
    }

    // Excel import preview / confirm
    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = { Text(L10n.s.setImportExcelTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(L10n.s.setImportFound(preview.rowCount), style = MoneyTypography.bodyMedium)
                    Text(L10n.s.setImportPeriod(preview.dateRange), style = MoneyTypography.bodyMedium)
                    Text(
                        L10n.s.setImportCounts(
                            preview.incomeCount,
                            preview.expenseCount,
                            preview.savingsCount,
                            preview.savingsSpendCount
                        ),
                        style = MoneyTypography.bodyMedium
                    )
                    Text(
                        L10n.s.setImportTotal("%.2f".format(preview.totalAmount)),
                        style = MoneyTypography.bodyMedium
                    )
                    Text(
                        L10n.s.setImportDupes,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmImportExcel() }) {
                    Text(L10n.s.importBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportPreview() }) {
                    Text(L10n.s.cancel)
                }
            }
        )
    }

    // Restore confirmation before opening the file picker
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(L10n.s.setRestoreTitle) },
            text = {
                Text(
                    L10n.s.setRestoreText,
                    style = MoneyTypography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        openBackupLauncher.launch(
                            arrayOf("application/json", "application/octet-stream", "*/*")
                        )
                    }
                ) { Text(L10n.s.chooseFile) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text(L10n.s.cancel) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress indicator for file/network operations
            if (isWorking) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Account section
            item {
                SettingsSection(title = L10n.s.setAccount, icon = Icons.Rounded.Person) {
                    SettingsTextField(
                        label = L10n.s.setUsername,
                        value = username,
                        onValueChange = { viewModel.setUsername(it) }
                    )
                    SettingsTextField(
                        label = L10n.s.setCurrency,
                        value = currency,
                        onValueChange = { viewModel.setCurrency(it) }
                    )
                }
            }

            // Google sync section
            item {
                SettingsSection(title = L10n.s.setSync, icon = Icons.Rounded.Cloud) {
                    if (!googleConnected) {
                        // v1.7.1: one-time Google Sheets setup — a plain-language
                        // 5-step guide replaces the raw "OAuth Client ID" text.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { oauthExpanded = !oauthExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(L10n.s.setGoogleSheets, style = MoneyTypography.labelLarge)
                                Text(
                                    L10n.s.setGoogleSheetsSub,
                                    style = MoneyTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (oauthExpanded)
                                    Icons.Rounded.ExpandLess
                                else
                                    Icons.Rounded.ExpandMore,
                                contentDescription = L10n.s.setExpand,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedVisibility(visible = oauthExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                GoogleSetupGuide()
                                SettingsTextField(
                                    label = L10n.s.setClientId,
                                    value = clientId,
                                    onValueChange = { viewModel.setGoogleClientId(it) }
                                )
                                // v1.8.0: optional — sync into the user's existing sheet
                                SettingsTextField(
                                    label = L10n.s.setSpreadsheetLink,
                                    value = spreadsheetLink,
                                    onValueChange = { viewModel.setGoogleSpreadsheetLink(it) }
                                )
                                Text(
                                    L10n.s.setSpreadsheetLinkHint,
                                    style = MoneyTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.connectGoogle() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(L10n.s.setConnectGoogle, style = MoneyTypography.labelLarge)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        L10n.s.setConnected,
                                        style = MoneyTypography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = incomeTextColor(),
                                        maxLines = 1
                                    )
                                    Text(
                                        L10n.s.setSpreadsheet(spreadsheetId),
                                        style = MoneyTypography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { viewModel.syncNow() }) {
                                        Text(L10n.s.setSyncNow, style = MoneyTypography.labelMedium)
                                    }
                                    TextButton(onClick = { viewModel.disconnectGoogle() }) {
                                        Text(L10n.s.setDisconnect, color = expenseTextColor(), style = MoneyTypography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notifications section
            item {
                SettingsSection(title = L10n.s.setNotifications, icon = Icons.Rounded.Notifications) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleReminder(!reminderEnabled) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            L10n.s.setDailyReminder,
                            style = MoneyTypography.bodyLarge,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { viewModel.toggleReminder(it) }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            L10n.s.setReminderTime,
                            style = MoneyTypography.bodyLarge,
                            color = if (reminderEnabled)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "%02d:%02d".format(reminderHour, reminderMinute),
                            style = MoneyTypography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Appearance section
            item {
                SettingsSection(title = L10n.s.setAppearance, icon = Icons.Rounded.Palette) {
                    val themes = listOf(
                        "system" to L10n.s.themeSystem,
                        "light" to L10n.s.themeLight,
                        "dark" to L10n.s.themeDark
                    )
                    themes.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setTheme(value) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MoneyTypography.bodyLarge)
                            RadioButton(
                                selected = themeMode == value,
                                onClick = { viewModel.setTheme(value) }
                            )
                        }
                    }

                    // v1.9.0: in-app language switch (RU / EN), mirrors the
                    // theme radios above
                    Text(
                        L10n.s.setLanguage,
                        style = MoneyTypography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    listOf("ru" to L10n.s.langRu, "en" to L10n.s.langEn).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setLanguage(value) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MoneyTypography.bodyLarge)
                            RadioButton(
                                selected = language == value,
                                onClick = { viewModel.setLanguage(value) }
                            )
                        }
                    }
                }
            }

            // Monthly budget (v1.6.0)
            item {
                SettingsSection(title = L10n.s.setMonthlyBudget, icon = Icons.Rounded.AccountBalance) {
                    // (1) Fixed cap in ₽/month (blank = off)
                    SettingsTextField(
                        label = L10n.s.setCapLimit,
                        value = capText,
                        onValueChange = {
                            capText = it
                            viewModel.setMonthlyCapText(it)
                        }
                    )
                    Text(
                        L10n.s.setCapLimitHint,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // (2) Link to income: N% of the month's income
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            L10n.s.setCapLinkIncome,
                            style = MoneyTypography.bodyLarge,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Switch(
                            checked = capLinkedIncome,
                            onCheckedChange = { viewModel.setCapLinkedIncome(it) }
                        )
                    }
                    AnimatedVisibility(visible = capLinkedIncome) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                L10n.s.setCapIncomePct,
                                style = MoneyTypography.bodyLarge,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            OutlinedTextField(
                                value = pctText,
                                onValueChange = {
                                    val digits = it.filter { c -> c.isDigit() }
                                    pctText = digits
                                    viewModel.setCapIncomePctText(digits)
                                },
                                modifier = Modifier.width(110.dp),
                                singleLine = true,
                                textStyle = MoneyTypography.labelLarge,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // (3) Category templates (per-month planned amounts)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTemplates = !showTemplates },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            L10n.s.setTemplateGroup,
                            style = MoneyTypography.bodyLarge
                        )
                        Icon(
                            imageVector = if (showTemplates) {
                                Icons.Rounded.ExpandLess
                            } else {
                                Icons.Rounded.ExpandMore
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = showTemplates) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TemplateGroup(
                                title = L10n.s.txTypeIncome,
                                color = incomeTextColor(),
                                categories = DefaultCategories.getByType(TransactionType.INCOME),
                                templates = budgetTemplates,
                                type = BudgetType.INCOME,
                                onAmount = viewModel::setTemplateAmount
                            )
                            TemplateGroup(
                                title = L10n.s.txTypeExpense,
                                color = expenseTextColor(),
                                categories = DefaultCategories.getByType(TransactionType.EXPENSE),
                                templates = budgetTemplates,
                                type = BudgetType.EXPENSE,
                                onAmount = viewModel::setTemplateAmount
                            )
                            TemplateGroup(
                                title = L10n.s.txTypeSavings,
                                color = savingsTextColor(),
                                categories = DefaultCategories.getByType(TransactionType.SAVINGS),
                                templates = budgetTemplates,
                                type = BudgetType.SAVINGS,
                                onAmount = viewModel::setTemplateAmount
                            )
                        }
                    }

                    // (4) Copy the previous month's plan — only while there are
                    // no templates (templates take priority, this is the fallback)
                    if (budgetTemplates.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                L10n.s.setCopyPrevMonth,
                                style = MoneyTypography.bodyLarge,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Switch(
                                checked = budgetCopyPrevMonth,
                                onCheckedChange = { viewModel.setBudgetCopyPrevMonth(it) }
                            )
                        }
                    }
                }
            }

            // Data section
            item {
                SettingsSection(title = L10n.s.setData, icon = Icons.Rounded.FileOpen) {
                    Button(
                        onClick = { createExcelLauncher.launch("moneyapp-exports.xlsx") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L10n.s.setExportExcel, style = MoneyTypography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = {
                            openExcelLauncher.launch(
                                arrayOf(XLSX_MIME, "application/vnd.ms-excel", "*/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L10n.s.setImportExcel, style = MoneyTypography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = { createBackupLauncher.launch("moneyapp-backup.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L10n.s.setBackup, style = MoneyTypography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = { showRestoreConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L10n.s.setRestore, style = MoneyTypography.labelLarge)
                    }
                }
            }

            // Categories
            item {
                SettingsSection(title = L10n.s.setCategories, icon = Icons.Rounded.Category) {
                    Text(
                        L10n.s.catSortHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MoneyTypography.bodySmall
                    )
                    FilledTonalButton(
                        onClick = onNavigateToCategories,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L10n.s.catManageTitle, style = MoneyTypography.labelLarge)
                    }
                }
            }

            // About
            item {
                SettingsSection(title = L10n.s.setAbout, icon = Icons.Rounded.Info) {
                    Text(
                        L10n.s.setVersion(BuildConfig.VERSION_NAME),
                        style = MoneyTypography.bodyMedium
                    )
                    Text(
                        L10n.s.setTagline,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        L10n.s.setGmsFree,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Last fatal crash journal (v1.6.0)
                    if (lastCrash.isNotBlank()) {
                        Text(
                            L10n.s.setLastCrash,
                            style = MoneyTypography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = expenseTextColor()
                        )
                        Text(
                            lastCrash.lineSequence().take(12).joinToString("\n"),
                            style = MoneyTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 12
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { viewModel.shareCrashLog() }) {
                                Text(L10n.s.setShareCrash, style = MoneyTypography.labelMedium)
                            }
                            TextButton(onClick = { viewModel.clearCrashLog() }) {
                                Text(L10n.s.setHide, style = MoneyTypography.labelMedium)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MoneyTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/** kopecks → user-editable text (0 → blank). */
private fun kopecksToText(kopecks: Long): String {
    if (kopecks <= 0) return ""
    val rest = kopecks % 100
    return if (rest == 0L) (kopecks / 100).toString()
    else String.format(java.util.Locale.ROOT, "%.2f", kopecks / 100.0)
}

// v1.7.1: one-time Google Cloud Console setup in 5 steps. Only the first line
// is interactive (opens the console in the browser); the app does the rest —
// it creates the spreadsheet itself and hides this block once connected.
@Composable
private fun GoogleSetupGuide() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://console.cloud.google.com/"))
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Google Cloud Console",
                style = MoneyTypography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        GuideStep(1, L10n.s.guideStep1)
        GuideStep(2, L10n.s.guideStep2)
        GuideStep(3, L10n.s.guideStep3)
        GuideStep(4, L10n.s.guideStep4)
        GuideStep(5, L10n.s.guideStep5)
    }
}

@Composable
private fun GuideStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$number.",
            style = MoneyTypography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(14.dp)
        )
        Text(
            text = text,
            style = MoneyTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// v1.6.0: one template group (Доход / Расход / Отложить)
@Composable
private fun TemplateGroup(
    title: String,
    color: Color,
    categories: List<Category>,
    templates: List<BudgetTemplate>,
    type: BudgetType,
    onAmount: (String, BudgetType, String) -> Unit
) {
    // Expenses group is expanded by default (most templates live there)
    var expanded by remember { mutableStateOf(type == BudgetType.EXPENSE) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MoneyTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { category ->
                    TemplateRow(
                        name = category.name,
                        amountKopecks = templates
                            .find { it.category == category.name && it.type == type }
                            ?.amountKopecks ?: 0L,
                        type = type,
                        onAmount = onAmount
                    )
                }
            }
        }
    }
}

// One template line: category name + amount field (blank removes the template)
@Composable
private fun TemplateRow(
    name: String,
    amountKopecks: Long,
    type: BudgetType,
    onAmount: (String, BudgetType, String) -> Unit
) {
    var text by remember { mutableStateOf(kopecksToText(amountKopecks)) }
    LaunchedEffect(amountKopecks) { text = kopecksToText(amountKopecks) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MoneyTypography.bodyMedium,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onAmount(name, type, it)
            },
            modifier = Modifier.width(110.dp),
            singleLine = true,
            textStyle = MoneyTypography.labelMedium
        )
    }
}
