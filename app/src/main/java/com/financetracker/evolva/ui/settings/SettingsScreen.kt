package com.financetracker.evolva.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.AppConstants
import com.financetracker.evolva.data.backup.BackupManager
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.prefs.PasswordChangeResult
import com.financetracker.evolva.data.security.DeviceCredentialAuth
import com.financetracker.evolva.data.templates.TEMPLATES
import com.financetracker.evolva.ui.ImportResult
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.TypeTag
import com.financetracker.evolva.ui.theme.AppThemeOption
import com.financetracker.evolva.ui.theme.FinanceColors
import com.financetracker.evolva.ui.theme.palette
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

private enum class SettingsTab(val label: String) {
    General("General"),
    Data("Data"),
    Backup("Backup"),
    Security("Security"),
    About("About")
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = SettingsTab.entries

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = FinanceColors.Text,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = FinanceColors.Background,
            contentColor = FinanceColors.Text
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tab.label) }
                )
            }
        }

        when (tabs[selectedTab]) {
            SettingsTab.General -> GeneralSettingsTab(viewModel)
            SettingsTab.Data -> DataSettingsTab(viewModel)
            SettingsTab.Backup -> BackupSettingsTab(viewModel)
            SettingsTab.Security -> SecuritySettingsTab(viewModel)
            SettingsTab.About -> AboutSettingsTab()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneralSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsState()
    val filterAutoCloseSeconds by viewModel.filterAutoCloseSeconds.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()
    val autoCloseOptions = listOf(0, 5, 7, 10, 15, 30)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setBudgetAlertsEnabled(granted)
        if (granted) {
            com.financetracker.evolva.data.notify.BudgetAlertNotifier.ensureChannel(context)
            viewModel.refreshBudgetAlerts(context)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ThemePickerSection(
                selected = appTheme,
                onSelect = viewModel::setAppTheme
            )
        }
        item {
            SectionCard(title = "Currency") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCurrency.entries.forEach { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick = { viewModel.setCurrency(c) },
                            label = { Text("${c.symbol} ${c.code}") }
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = "Budget alerts") {
                Text(
                    "Get a notification when a category reaches 80% of its monthly limit or goes over.",
                    fontSize = 13.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Near / over budget", fontSize = 14.sp, color = FinanceColors.Text)
                    Switch(
                        checked = budgetAlertsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    viewModel.setBudgetAlertsEnabled(true)
                                    com.financetracker.evolva.data.notify.BudgetAlertNotifier.ensureChannel(context)
                                    viewModel.refreshBudgetAlerts(context)
                                }
                            } else {
                                viewModel.setBudgetAlertsEnabled(false)
                            }
                        }
                    )
                }
            }
        }
        item {
            SectionCard(title = "Date filter auto-hide") {
                Text(
                    "Collapse the date filter chips after idle time. Set to Off to keep them open.",
                    fontSize = 13.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    autoCloseOptions.forEach { seconds ->
                        FilterChip(
                            selected = filterAutoCloseSeconds == seconds,
                            onClick = { viewModel.setFilterAutoCloseSeconds(seconds) },
                            label = {
                                Text(
                                    text = if (seconds == 0) "Off" else "${seconds}s",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePickerSection(
    selected: AppThemeOption,
    onSelect: (AppThemeOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(FinanceColors.Header, FinanceColors.Accent.copy(alpha = 0.85f))
                )
            )
            .padding(16.dp)
    ) {
        Text(
            "Themes",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = FinanceColors.OnHeader
        )
        Text(
            "Pick a look that fits your money mood",
            fontSize = 12.sp,
            color = FinanceColors.OnHeader.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AppThemeOption.entries) { theme ->
                ThemeSwatchCard(
                    theme = theme,
                    selected = theme == selected,
                    onClick = { onSelect(theme) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatchCard(
    theme: AppThemeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = theme.palette()
    Column(
        modifier = Modifier
            .width(118.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.accent else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(listOf(palette.header, palette.accent))
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ColorDot(color = palette.income)
                ColorDot(color = palette.expense)
                ColorDot(color = palette.savings)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            theme.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.text
        )
        Text(
            theme.subtitle,
            fontSize = 11.sp,
            color = palette.textSoft,
            maxLines = 2
        )
    }
}


@Composable
private fun ColorDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun DataSettingsTab(viewModel: MainViewModel) {
    val recurringRules by viewModel.recurringRules.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val customExpenseCategories by viewModel.customExpenseCategories.collectAsState()
    var newExpenseType by remember { mutableStateOf("") }
    var editingRule by remember { mutableStateOf<RecurringRule?>(null) }
    fun fmt(v: Double) = formatAmount(v, currency)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = "Custom expense types") {
                Text(
                    "Add your own expense categories. They appear in Transactions and Budgets, and are included in backup/restore.",
                    fontSize = 12.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newExpenseType,
                        onValueChange = { newExpenseType = it },
                        label = { Text("New type") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val name = newExpenseType.trim()
                            if (name.isNotEmpty()) {
                                viewModel.addCustomExpenseCategory(name) { ok ->
                                    if (ok) newExpenseType = ""
                                }
                            }
                        }
                    ) { Text("Add") }
                }
                if (customExpenseCategories.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No custom types yet. Built-in types (Food, Transport, …) stay available.",
                        fontSize = 12.sp,
                        color = FinanceColors.TextSoft
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    customExpenseCategories.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 13.5.sp, color = FinanceColors.Text)
                            TextButton(onClick = { viewModel.removeCustomExpenseCategory(name) }) {
                                Text("Remove", color = FinanceColors.Expense)
                            }
                        }
                        HorizontalDivider(color = FinanceColors.Border)
                    }
                }
            }
        }

        item {
            Text(
                "Starter Templates",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinanceColors.Text
            )
        }
        TEMPLATES.forEach { template ->
            item(key = template.id) {
                SectionCard(title = template.label) {
                    Text(
                        template.description,
                        fontSize = 12.sp,
                        color = FinanceColors.TextSoft
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = { viewModel.loadTemplate(template) }) {
                        Text("Load template")
                    }
                }
            }
        }

        item {
            SectionCard(title = "Recurring Transactions") {
                if (recurringRules.isEmpty()) {
                    Text(
                        "No recurring transactions yet. Turn on \"Repeat monthly\" when adding a transaction to create one.",
                        fontSize = 13.sp, color = FinanceColors.TextSoft
                    )
                } else {
                    recurringRules.forEach { rule ->
                        RecurringRuleRow(
                            rule = rule,
                            formattedAmount = fmt(rule.amount),
                            onEdit = { editingRule = rule },
                            onToggleActive = { viewModel.toggleRecurringActive(rule) },
                            onDelete = { viewModel.deleteRecurringRule(rule.id) }
                        )
                        HorizontalDivider(color = FinanceColors.Border)
                    }
                }
            }
        }
    }

    editingRule?.let { rule ->
        EditRecurringRuleSheet(
            rule = rule,
            customExpenseCategories = customExpenseCategories,
            onDismiss = { editingRule = null },
            onSave = {
                viewModel.updateRecurringRule(it)
                editingRule = null
            },
            onDelete = {
                viewModel.deleteRecurringRule(rule.id)
                editingRule = null
            }
        )
    }
}

@Composable
private fun BackupSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var pendingReplace by remember { mutableStateOf(false) }

    val createJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(viewModel.exportBackupJson().toByteArray())
            }
        }
    }
    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(viewModel.exportCsv().toByteArray())
            }
        }
    }
    val openJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (text != null) {
                when (val result = viewModel.importBackup(text, pendingReplace)) {
                    is ImportResult.Success -> importResultMessage = "Imported ${result.transactionCount} transactions."
                    ImportResult.Invalid -> importResultMessage = "That file doesn't look like a Finance Tracker backup."
                }
            } else {
                importResultMessage = "Couldn't read that file."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = "Backup, Restore & Share") {
                Text(
                    "Export a backup file to keep a copy of your data or send it to a family member. Import a backup to restore it, or merge someone else's data into yours.",
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { createJsonLauncher.launch(BackupManager.backupFileName()) }) {
                        Text("Export JSON")
                    }
                    OutlinedButton(onClick = { createCsvLauncher.launch(BackupManager.csvFileName()) }) {
                        Text("Export CSV")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pendingReplace = false
                        openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("Import & Merge") }
                    OutlinedButton(onClick = {
                        pendingReplace = true
                        openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("Import & Replace") }
                }
                importResultMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 12.5.sp, color = FinanceColors.Savings)
                }
            }
        }
    }
}

@Composable
private fun SecuritySettingsTab(viewModel: MainViewModel) {
    val hasPassword by viewModel.hasAppPassword.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var passwordDialogError by remember { mutableStateOf<String?>(null) }
    var dangerError by remember { mutableStateOf<String?>(null) }

    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearAllTransactions()
            showClearConfirm = false
            dangerError = null
        } else {
            dangerError = "Device confirmation was cancelled or failed."
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = "App Password") {
                Text(
                    if (hasPassword) {
                        "An app password is set. It is required for Danger Zone actions."
                    } else {
                        "No app password is set (default is blank). You can set one to protect sensitive actions."
                    },
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    passwordDialogError = null
                    showPasswordDialog = true
                }) {
                    Text(if (hasPassword) "Change Password" else "Set Password")
                }
                statusMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        fontSize = 12.5.sp,
                        color = if (statusIsError) FinanceColors.Expense else FinanceColors.Savings
                    )
                }
            }
        }

        item {
            SectionCard(title = "Danger Zone") {
                Text(
                    "Remove every transaction and start fresh. Budgets and recurring rules are kept. " +
                        "This requires your device screen lock and your app password.",
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        dangerError = null
                        showClearConfirm = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceColors.Expense)
                ) { Text("Clear All Transactions") }
            }
        }
    }

    if (showPasswordDialog) {
        PasswordDialog(
            hasExistingPassword = hasPassword,
            error = passwordDialogError,
            onDismiss = {
                showPasswordDialog = false
                passwordDialogError = null
            },
            onSubmit = { current, newPassword, clear ->
                viewModel.changeAppPassword(
                    currentPassword = current,
                    newPassword = if (clear) "" else newPassword
                ) { result ->
                    when (result) {
                        PasswordChangeResult.Success -> {
                            statusIsError = false
                            statusMessage = when {
                                clear -> "App password cleared."
                                hasPassword -> "App password updated."
                                else -> "App password set."
                            }
                            passwordDialogError = null
                            showPasswordDialog = false
                        }
                        PasswordChangeResult.WrongCurrentPassword -> {
                            passwordDialogError = "Current password is incorrect."
                        }
                    }
                }
            }
        )
    }

    if (showClearConfirm) {
        DangerZoneConfirmDialog(
            hasAppPassword = hasPassword,
            error = dangerError,
            onDismiss = {
                showClearConfirm = false
                dangerError = null
            },
            onContinue = { appPassword ->
                scope.launch {
                    val passwordOk = viewModel.verifyAppPassword(appPassword)
                    if (!passwordOk) {
                        dangerError = "App password is incorrect."
                        return@launch
                    }
                    val intent = DeviceCredentialAuth.createConfirmIntent(
                        activity = activity,
                        title = "Confirm device identity",
                        description = "Unlock with your device PIN, pattern, or password to continue."
                    )
                    if (intent == null) {
                        dangerError = "Set a screen lock (PIN, pattern, or password) on this device first."
                        return@launch
                    }
                    deviceCredentialLauncher.launch(intent)
                }
            }
        )
    }
}

@Composable
private fun AboutSettingsTab() {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = "About") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = AppConstants.APP_NAME,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            AppConstants.APP_NAME,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinanceColors.Text
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Version ${AppConstants.APP_VERSION}",
                            fontSize = 13.sp,
                            color = FinanceColors.TextSoft
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            AppConstants.DEVELOPER_NAME,
                            fontSize = 13.sp,
                            color = FinanceColors.TextSoft
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showAbout = true }) {
                    Text("About Box")
                }
            }
        }

        item {
            SectionCard(title = "Exit App") {
                Text(
                    "Close Finance Tracker completely.",
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showExitConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceColors.Expense)
                ) {
                    Text("Exit App")
                }
            }
        }
    }

    if (showAbout) {
        AboutBoxDialog(onDismiss = { showAbout = false })
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit App?") },
            text = { Text("Finance Tracker will close.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    (context as? Activity)?.finishAffinity()
                    exitProcess(0)
                }) { Text("Exit", color = FinanceColors.Expense) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AboutBoxDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppConstants.APP_NAME) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = AppConstants.APP_NAME,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Version ${AppConstants.APP_VERSION}",
                    fontSize = 14.sp,
                    color = FinanceColors.Text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Developer", fontSize = 12.sp, color = FinanceColors.TextSoft)
                Text(
                    AppConstants.DEVELOPER_NAME,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinanceColors.Text
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
private fun PasswordDialog(
    hasExistingPassword: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (current: String, newPassword: String, clear: Boolean) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExistingPassword) "Change App Password" else "Set App Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasExistingPassword) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm new password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                shownError?.let { Text(it, fontSize = 12.5.sp, color = FinanceColors.Expense) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newPassword != confirmPassword) {
                    localError = "New passwords do not match."
                    return@TextButton
                }
                if (newPassword.isEmpty()) {
                    localError = "Enter a new password, or use Clear Password."
                    return@TextButton
                }
                localError = null
                onSubmit(currentPassword, newPassword, false)
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (hasExistingPassword) {
                    TextButton(onClick = {
                        localError = null
                        onSubmit(currentPassword, "", true)
                    }) { Text("Clear Password", color = FinanceColors.Expense) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun DangerZoneConfirmDialog(
    hasAppPassword: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onContinue: (String) -> Unit
) {
    var appPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear all transactions?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This permanently deletes every transaction. Budgets and recurring rules are kept. " +
                        "Confirm with your app password, then your device screen lock.",
                    fontSize = 13.sp,
                    color = FinanceColors.TextSoft
                )
                OutlinedTextField(
                    value = appPassword,
                    onValueChange = { appPassword = it },
                    label = {
                        Text(
                            if (hasAppPassword) "App password" else "App password (blank if unset)"
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, fontSize = 12.5.sp, color = FinanceColors.Expense) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onContinue(appPassword) }) {
                Text("Continue", color = FinanceColors.Expense)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RecurringRuleRow(
    rule: RecurringRule,
    formattedAmount: String,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TypeTag(rule.type)
            Spacer(modifier = Modifier.height(4.dp))
            val label = if (rule.type == TransactionType.TRANSFER) {
                val arrow = if (rule.direction == TransferDirection.OUT) "↗" else "↙"
                "$arrow ${rule.category}"
            } else rule.category
            Text(label, fontSize = 13.5.sp, color = FinanceColors.Text)
            Text("Day ${rule.day} of each month · $formattedAmount", fontSize = 12.sp, color = FinanceColors.TextSoft)
            if (!rule.active) {
                Text("Paused", fontSize = 11.5.sp, color = FinanceColors.Warn)
            }
        }
        TextButton(onClick = onEdit) {
            Text("Edit")
        }
        Switch(checked = rule.active, onCheckedChange = { onToggleActive() })
        TextButton(onClick = onDelete) {
            Text("Delete", color = FinanceColors.Expense)
        }
    }
}

