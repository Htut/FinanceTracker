package com.financetracker.evolva.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
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
import com.financetracker.evolva.data.model.AppLanguage
import com.financetracker.evolva.data.model.AutoLockRule
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.model.formatExchangeRate
import com.financetracker.evolva.data.prefs.PasswordChangeResult
import com.financetracker.evolva.data.profile.ProfileIds
import com.financetracker.evolva.data.profile.ProfileKind
import com.financetracker.evolva.data.profile.TrackerProfile
import com.financetracker.evolva.data.security.BiometricAuth
import com.financetracker.evolva.data.security.DeviceCredentialAuth
import com.financetracker.evolva.data.templates.AppTemplate
import com.financetracker.evolva.data.templates.TEMPLATES
import com.financetracker.evolva.ui.ImportResult
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.accounts.AccountsSection
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.TypeTag
import com.financetracker.evolva.ui.theme.AppThemeOption
import com.financetracker.evolva.ui.theme.FinanceColors
import com.financetracker.evolva.ui.theme.palette
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

private enum class SettingsTab(val labelRes: Int) {
    General(R.string.settings_tab_general),
    Data(R.string.settings_tab_data),
    Backup(R.string.settings_tab_backup),
    Security(R.string.settings_tab_security),
    About(R.string.settings_tab_about)
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = SettingsTab.entries
    val activeProfile by viewModel.activeProfile.collectAsState()
    val activeProfileLabel = if (activeProfile.kind == ProfileKind.PERSONAL) {
        stringResource(R.string.profile_my_tracker)
    } else {
        activeProfile.displayName
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.settings_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = FinanceColors.Text,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp)
        )
        Text(
            stringResource(R.string.active_profile_label, activeProfileLabel),
            fontSize = 13.sp,
            color = FinanceColors.TextSoft,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
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
                    text = { Text(stringResource(tab.labelRes)) }
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

@Composable
private fun GeneralSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsState()
    val language by viewModel.language.collectAsState()
    val filterAutoCloseSeconds by viewModel.filterAutoCloseSeconds.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()
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
            SectionCard(title = stringResource(R.string.section_language)) {
                Text(
                    stringResource(
                        R.string.language_currency_preset_help,
                        "${language.suggestedCurrency.symbol} ${language.suggestedCurrency.code}"
                    ),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppLanguage.entries.toList(), key = { it.tag }) { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = {
                                Text(
                                    text = lang.nativeLabel,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.section_currency)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppCurrency.entries.toList(), key = { it.code }) { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick = { viewModel.setCurrency(c) },
                            label = {
                                Text(
                                    text = "${c.symbol} ${c.code}",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }
        item {
            ExchangeRatesSection(
                homeCurrency = currency,
                exchangeRates = exchangeRates,
                onSave = viewModel::setExchangeRate,
                onFetchLive = viewModel::fetchLiveExchangeRates
            )
        }
        item {
            SectionCard(title = stringResource(R.string.section_budget_alerts)) {
                Text(
                    stringResource(R.string.budget_alerts_help),
                    fontSize = 13.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.budget_alerts_toggle),
                        fontSize = 14.sp,
                        color = FinanceColors.Text
                    )
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
            SectionCard(title = stringResource(R.string.section_date_filter_autohide)) {
                Text(
                    stringResource(R.string.date_filter_autohide_help),
                    fontSize = 13.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(autoCloseOptions, key = { it }) { seconds ->
                        FilterChip(
                            selected = filterAutoCloseSeconds == seconds,
                            onClick = { viewModel.setFilterAutoCloseSeconds(seconds) },
                            label = {
                                Text(
                                    text = if (seconds == 0) {
                                        stringResource(R.string.filter_off)
                                    } else {
                                        "${seconds}s"
                                    },
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
private fun ExchangeRatesSection(
    homeCurrency: AppCurrency,
    exchangeRates: Map<String, Double>,
    onSave: (String, Double) -> Unit,
    onFetchLive: () -> Unit
) {
    var toHome by remember(homeCurrency, exchangeRates) {
        mutableStateOf(
            AppCurrency.entries
                .filter { it != homeCurrency }
                .associate { currency ->
                    val rate = exchangeRates[currency.code]
                    currency.code to (rate?.let { formatExchangeRate(it) } ?: "")
                }
        )
    }
    var toForeign by remember(homeCurrency, exchangeRates) {
        mutableStateOf(
            AppCurrency.entries
                .filter { it != homeCurrency }
                .associate { currency ->
                    val rate = exchangeRates[currency.code]
                    currency.code to (
                        rate?.takeIf { it > 0 }?.let { formatExchangeRate(1.0 / it) } ?: ""
                    )
                }
        )
    }

    fun onToHomeChanged(code: String, input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        toHome = toHome + (code to filtered)
        filtered.toDoubleOrNull()?.takeIf { it > 0 }?.let { rate ->
            toForeign = toForeign + (code to formatExchangeRate(1.0 / rate))
        }
    }

    fun onToForeignChanged(code: String, input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        toForeign = toForeign + (code to filtered)
        filtered.toDoubleOrNull()?.takeIf { it > 0 }?.let { inverse ->
            toHome = toHome + (code to formatExchangeRate(1.0 / inverse))
        }
    }

    SectionCard(title = stringResource(R.string.section_exchange_rates)) {
        Text(
            stringResource(R.string.exchange_rates_help),
            fontSize = 12.sp,
            color = FinanceColors.TextSoft
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onFetchLive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.fetch_live_rates))
        }
        Spacer(modifier = Modifier.height(10.dp))
        AppCurrency.entries.filter { it != homeCurrency }.forEach { foreign ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = toHome[foreign.code].orEmpty(),
                        onValueChange = { onToHomeChanged(foreign.code, it) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.rate_one_in_home,
                                    foreign.code,
                                    homeCurrency.code
                                )
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            toHome[foreign.code]?.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                                onSave(foreign.code, it)
                            }
                        },
                        enabled = toHome[foreign.code]?.toDoubleOrNull()?.let { it > 0 } == true
                    ) { Text(stringResource(R.string.action_save)) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = toForeign[foreign.code].orEmpty(),
                    onValueChange = { onToForeignChanged(foreign.code, it) },
                    label = {
                        Text(
                            stringResource(
                                R.string.rate_one_in_home,
                                homeCurrency.code,
                                foreign.code
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
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
            stringResource(R.string.section_themes),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = FinanceColors.OnHeader
        )
        Text(
            stringResource(R.string.themes_subtitle),
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
            stringResource(theme.labelRes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.text
        )
        Text(
            stringResource(theme.subtitleRes),
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
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
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
            AccountsSection(
                accounts = accounts,
                transactions = transactions,
                currency = currency,
                onSave = viewModel::upsertAccount,
                onDelete = viewModel::deleteAccount
            )
        }
        item {
            SectionCard(title = stringResource(R.string.section_custom_expense_types)) {
                Text(
                    stringResource(R.string.custom_expense_help),
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
                        label = { Text(stringResource(R.string.new_type)) },
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
                    ) { Text(stringResource(R.string.action_add)) }
                }
                if (customExpenseCategories.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.custom_expense_empty),
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
                                Text(stringResource(R.string.action_remove), color = FinanceColors.Expense)
                            }
                        }
                        HorizontalDivider(color = FinanceColors.Border)
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.section_profiles),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinanceColors.Text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.profiles_help),
                fontSize = 12.sp,
                color = FinanceColors.TextSoft
            )
        }
        item {
            PersonalProfileCard(viewModel)
        }
        TEMPLATES.forEach { template ->
            item(key = template.id) {
                TemplateProfileCard(viewModel = viewModel, template = template)
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_recurring)) {
                if (recurringRules.isEmpty()) {
                    Text(
                        stringResource(R.string.recurring_empty),
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
private fun PersonalProfileCard(viewModel: MainViewModel) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isActive = activeProfile.id == ProfileIds.PERSONAL
    SectionCard(title = stringResource(R.string.profile_my_tracker)) {
        Text(
            stringResource(R.string.profile_personal_subtitle),
            fontSize = 12.sp,
            color = FinanceColors.TextSoft
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isActive) {
                Text(
                    stringResource(R.string.profile_active),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinanceColors.Savings
                )
            } else {
                Button(onClick = { viewModel.switchProfile(ProfileIds.PERSONAL) }) {
                    Text(stringResource(R.string.profile_switch))
                }
            }
        }
    }
}

@Composable
private fun TemplateProfileCard(
    viewModel: MainViewModel,
    template: AppTemplate
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val instance = profiles.find {
        it.kind == ProfileKind.TEMPLATE && it.templateId == template.id
    }
    var pendingDelete by remember { mutableStateOf<TrackerProfile?>(null) }

    SectionCard(title = stringResource(template.labelRes)) {
        Text(stringResource(template.descriptionRes), fontSize = 12.sp, color = FinanceColors.TextSoft)
        Spacer(modifier = Modifier.height(10.dp))
        if (instance == null) {
            Button(onClick = { viewModel.setupTemplateProfile(template) }) {
                Text(stringResource(R.string.profile_setup))
            }
        } else {
            val isActive = activeProfile.id == instance.id
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isActive) {
                    Text(
                        stringResource(R.string.profile_active),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Savings
                    )
                } else {
                    Button(onClick = { viewModel.switchProfile(instance.id) }) {
                        Text(stringResource(R.string.profile_switch))
                    }
                }
                OutlinedButton(
                    onClick = { pendingDelete = instance },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceColors.Expense)
                ) {
                    Text(stringResource(R.string.profile_delete))
                }
            }
        }
    }

    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.profile_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.profile_delete_confirm_body, profile.displayName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplateProfile(profile.id)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.profile_delete), color = FinanceColors.Expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun BackupSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var pendingReplace by remember { mutableStateOf(false) }
    var pendingCsv by remember { mutableStateOf(false) }
    var showDriveRestoreConfirm by remember { mutableStateOf(false) }

    val driveEnabled = AppConstants.ENABLE_GOOGLE_DRIVE_BACKUP
    val driveEmail by viewModel.driveAccountEmail.collectAsState()
    val driveMeta by viewModel.driveBackupMeta.collectAsState()
    val driveBusy by viewModel.driveBusy.collectAsState()

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
    val openImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (text != null) {
                val result = if (pendingCsv) {
                    viewModel.importCsv(text, pendingReplace)
                } else {
                    viewModel.importBackup(text, pendingReplace)
                }
                when (result) {
                    is ImportResult.Success -> importResultMessage =
                        appContext.getString(R.string.import_success, result.transactionCount)
                    ImportResult.Invalid -> importResultMessage =
                        appContext.getString(
                            if (pendingCsv) R.string.import_csv_invalid else R.string.import_invalid
                        )
                }
            } else {
                importResultMessage = appContext.getString(R.string.import_read_fail)
            }
        }
    }
    val driveAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleDriveAuthorizationResult(result.data)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (driveEnabled) {
            item {
                SectionCard(title = stringResource(R.string.section_google_drive)) {
                    Text(
                        stringResource(R.string.drive_help),
                        fontSize = 12.5.sp,
                        color = FinanceColors.TextSoft
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.drive_setup_help),
                        fontSize = 11.5.sp,
                        color = FinanceColors.TextSoft
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (driveEmail == null) {
                        Button(
                            onClick = {
                                viewModel.connectDrive { pendingIntent ->
                                    driveAuthLauncher.launch(
                                        IntentSenderRequest.Builder(pendingIntent).build()
                                    )
                                }
                            },
                            enabled = !driveBusy
                        ) {
                            Text(stringResource(R.string.drive_sign_in))
                        }
                    } else {
                        Text(
                            stringResource(R.string.drive_account, driveEmail!!),
                            fontSize = 13.sp,
                            color = FinanceColors.Text
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val metaText = driveMeta?.modifiedTime?.let {
                            stringResource(R.string.drive_last_backup, it)
                        } ?: stringResource(R.string.drive_no_cloud_backup_yet)
                        Text(metaText, fontSize = 12.sp, color = FinanceColors.TextSoft)
                        if (driveBusy) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.drive_busy),
                                fontSize = 12.sp,
                                color = FinanceColors.TextSoft
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = viewModel::backupToDrive,
                                enabled = !driveBusy
                            ) { Text(stringResource(R.string.drive_backup_now)) }
                            OutlinedButton(
                                onClick = { showDriveRestoreConfirm = true },
                                enabled = !driveBusy
                            ) { Text(stringResource(R.string.drive_restore)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = viewModel::signOutDrive,
                            enabled = !driveBusy
                        ) { Text(stringResource(R.string.drive_sign_out)) }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_backup)) {
                Text(
                    stringResource(R.string.backup_help),
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.backup_help_profile),
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { createJsonLauncher.launch(BackupManager.backupFileName()) }) {
                        Text(stringResource(R.string.export_json))
                    }
                    OutlinedButton(onClick = { createCsvLauncher.launch(BackupManager.csvFileName()) }) {
                        Text(stringResource(R.string.export_csv))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    stringResource(R.string.import_json_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinanceColors.Text
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pendingCsv = false
                        pendingReplace = false
                        openImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text(stringResource(R.string.import_merge)) }
                    OutlinedButton(onClick = {
                        pendingCsv = false
                        pendingReplace = true
                        openImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text(stringResource(R.string.import_replace)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.import_csv_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinanceColors.Text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.import_csv_help),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pendingCsv = true
                        pendingReplace = false
                        openImportLauncher.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")
                        )
                    }) { Text(stringResource(R.string.import_csv_merge)) }
                    OutlinedButton(onClick = {
                        pendingCsv = true
                        pendingReplace = true
                        openImportLauncher.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")
                        )
                    }) { Text(stringResource(R.string.import_csv_replace)) }
                }
                importResultMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, fontSize = 12.5.sp, color = FinanceColors.Savings)
                }
            }
        }
    }

    if (driveEnabled && showDriveRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showDriveRestoreConfirm = false },
            title = { Text(stringResource(R.string.drive_restore_confirm_title)) },
            text = { Text(stringResource(R.string.drive_restore_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDriveRestoreConfirm = false
                        viewModel.restoreFromDrive()
                    }
                ) { Text(stringResource(R.string.drive_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SecuritySettingsTab(viewModel: MainViewModel) {
    val hasPassword by viewModel.hasAppPassword.collectAsState()
    val viewOnly by viewModel.viewOnlyMode.collectAsState()
    val autoLockRule by viewModel.autoLockRule.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var passwordDialogError by remember { mutableStateOf<String?>(null) }
    var dangerError by remember { mutableStateOf<String?>(null) }

    val msgDeviceCancelled = stringResource(R.string.device_confirm_cancelled)
    val msgPasswordCleared = stringResource(R.string.password_cleared)
    val msgPasswordUpdated = stringResource(R.string.password_updated)
    val msgPasswordSet = stringResource(R.string.password_set)
    val msgIncorrectPassword = stringResource(R.string.incorrect_password)
    val msgConfirmDevice = stringResource(R.string.confirm_device_identity)
    val msgUnlockBiometricDesc = stringResource(R.string.unlock_biometric_desc)
    val msgSetScreenLock = stringResource(R.string.set_screen_lock_first)

    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearAllTransactions()
            showClearConfirm = false
            dangerError = null
        } else {
            dangerError = msgDeviceCancelled
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = stringResource(R.string.section_app_password)) {
                Text(
                    if (hasPassword) {
                        stringResource(R.string.app_password_set_help)
                    } else {
                        stringResource(R.string.app_password_unset_help)
                    },
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    passwordDialogError = null
                    showPasswordDialog = true
                }) {
                    Text(
                        if (hasPassword) stringResource(R.string.change_password)
                        else stringResource(R.string.set_password)
                    )
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
            SectionCard(title = stringResource(R.string.view_only_mode)) {
                Text(
                    stringResource(R.string.view_only_mode_help),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.view_only_mode),
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = FinanceColors.Text
                    )
                    Switch(
                        checked = viewOnly,
                        onCheckedChange = viewModel::setViewOnlyMode
                    )
                }
            }
        }

        item {
            val biometricEnabled by viewModel.biometricUnlockEnabled.collectAsState()
            val biometricAvailable = remember(context) {
                BiometricAuth.canAuthenticate(context)
            }
            SectionCard(title = stringResource(R.string.section_biometric_unlock)) {
                Text(
                    stringResource(R.string.biometric_unlock_help),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.biometric_unlock_toggle),
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = FinanceColors.Text
                    )
                    Switch(
                        checked = biometricEnabled && hasPassword,
                        enabled = hasPassword && biometricAvailable,
                        onCheckedChange = viewModel::setBiometricUnlockEnabled
                    )
                }
                if (!hasPassword) {
                    Text(
                        stringResource(R.string.biometric_requires_password),
                        fontSize = 11.5.sp,
                        color = FinanceColors.TextSoft,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (!biometricAvailable) {
                    Text(
                        stringResource(R.string.biometric_unavailable),
                        fontSize = 11.5.sp,
                        color = FinanceColors.TextSoft,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_auto_lock)) {
                Text(
                    stringResource(R.string.auto_lock_help),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            AutoLockRule.OFF to R.string.auto_lock_off,
                            AutoLockRule.AFTER_7_DAYS to R.string.auto_lock_7_days,
                            AutoLockRule.AFTER_30_DAYS to R.string.auto_lock_30_days,
                            AutoLockRule.AFTER_90_DAYS to R.string.auto_lock_90_days,
                            AutoLockRule.PREVIOUS_MONTHS to R.string.auto_lock_prev_months
                        ),
                        key = { it.first.days }
                    ) { (rule, labelRes) ->
                        FilterChip(
                            selected = autoLockRule == rule,
                            onClick = { viewModel.setAutoLockRule(rule) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_danger_zone)) {
                Text(
                    stringResource(R.string.danger_clear_help),
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        dangerError = null
                        showClearConfirm = true
                    },
                    enabled = !viewOnly,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceColors.Expense)
                ) { Text(stringResource(R.string.clear_all_transactions)) }
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
                                clear -> msgPasswordCleared
                                hasPassword -> msgPasswordUpdated
                                else -> msgPasswordSet
                            }
                            passwordDialogError = null
                            showPasswordDialog = false
                        }
                        PasswordChangeResult.WrongCurrentPassword -> {
                            passwordDialogError = msgIncorrectPassword
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
                        dangerError = msgIncorrectPassword
                        return@launch
                    }
                    val intent = DeviceCredentialAuth.createConfirmIntent(
                        activity = activity,
                        title = msgConfirmDevice,
                        description = msgUnlockBiometricDesc
                    )
                    if (intent == null) {
                        dangerError = msgSetScreenLock
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
    val appName = stringResource(R.string.app_name)
    val features = listOf(
        R.string.feature_profiles_title to R.string.feature_profiles_desc,
        R.string.feature_currency_title to R.string.feature_currency_desc,
        R.string.feature_wallets_title to R.string.feature_wallets_desc,
        R.string.feature_budgets_title to R.string.feature_budgets_desc,
        R.string.feature_transactions_title to R.string.feature_transactions_desc,
        R.string.feature_recurring_title to R.string.feature_recurring_desc,
        R.string.feature_reports_title to R.string.feature_reports_desc,
        R.string.feature_security_title to R.string.feature_security_desc,
        R.string.feature_widget_title to R.string.feature_widget_desc,
        R.string.feature_languages_title to R.string.feature_languages_desc
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(title = stringResource(R.string.section_about)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            appName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinanceColors.Text
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.version_label, AppConstants.APP_VERSION),
                            fontSize = 13.sp,
                            color = FinanceColors.TextSoft
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.developer_name),
                            fontSize = 13.sp,
                            color = FinanceColors.TextSoft
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    stringResource(R.string.about_tagline),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.beta_expires_notice),
                    fontSize = 12.sp,
                    color = FinanceColors.Warn
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showAbout = true }) {
                    Text(stringResource(R.string.about_box))
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_app_features)) {
                features.forEachIndexed { index, (titleRes, descRes) ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = FinanceColors.TextSoft.copy(alpha = 0.25f)
                        )
                    }
                    Text(
                        stringResource(titleRes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(descRes),
                        fontSize = 12.5.sp,
                        color = FinanceColors.TextSoft
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_exit_app)) {
                Text(
                    stringResource(R.string.exit_app_help),
                    fontSize = 12.5.sp,
                    color = FinanceColors.TextSoft
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showExitConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceColors.Expense)
                ) {
                    Text(stringResource(R.string.section_exit_app))
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
            title = { Text(stringResource(R.string.section_exit_app)) },
            text = { Text(stringResource(R.string.exit_app_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    (context as? Activity)?.finishAffinity()
                    exitProcess(0)
                }) { Text(stringResource(R.string.action_exit), color = FinanceColors.Expense) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun AboutBoxDialog(onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appName) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = appName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    stringResource(R.string.version_label, AppConstants.APP_VERSION),
                    fontSize = 14.sp,
                    color = FinanceColors.Text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.label_developer),
                    fontSize = 12.sp,
                    color = FinanceColors.TextSoft
                )
                Text(
                    stringResource(R.string.developer_name),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinanceColors.Text
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    stringResource(R.string.beta_expires_notice),
                    fontSize = 13.sp,
                    color = FinanceColors.Warn,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
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
    val mismatchError = stringResource(R.string.passwords_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (hasExistingPassword) stringResource(R.string.change_password)
                else stringResource(R.string.set_password)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasExistingPassword) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(stringResource(R.string.current_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
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
                    localError = mismatchError
                    return@TextButton
                }
                if (newPassword.isEmpty()) {
                    return@TextButton
                }
                localError = null
                onSubmit(currentPassword, newPassword, false)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (hasExistingPassword) {
                    TextButton(onClick = {
                        localError = null
                        onSubmit(currentPassword, "", true)
                    }) {
                        Text(stringResource(R.string.clear_password), color = FinanceColors.Expense)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

@Composable
private fun DangerZoneConfirmDialog(
    @Suppress("UNUSED_PARAMETER") hasAppPassword: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onContinue: (String) -> Unit
) {
    var appPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_all_transactions)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.danger_clear_help),
                    fontSize = 13.sp,
                    color = FinanceColors.TextSoft
                )
                OutlinedTextField(
                    value = appPassword,
                    onValueChange = { appPassword = it },
                    label = { Text(stringResource(R.string.app_password_label)) },
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
                Text(stringResource(R.string.action_continue), color = FinanceColors.Expense)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
            Text(
                stringResource(R.string.recurring_day_of_month, rule.day, formattedAmount),
                fontSize = 12.sp,
                color = FinanceColors.TextSoft
            )
            if (!rule.active) {
                Text(
                    stringResource(R.string.recurring_paused),
                    fontSize = 11.5.sp,
                    color = FinanceColors.Warn
                )
            }
        }
        TextButton(onClick = onEdit) {
            Text(stringResource(R.string.action_edit))
        }
        Switch(checked = rule.active, onCheckedChange = { onToggleActive() })
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.action_delete), color = FinanceColors.Expense)
        }
    }
}

